package com.pixel.domain.photo.moderation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.pixel.domain.photo.moderation.ClaudeImageModerator.Judgement;
import com.pixel.domain.photo.moderation.ModerationResult.Decision;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClaudeImageModeratorTest {

  @ParameterizedTest
  @CsvSource({
    "PUBLIC,NONE,PUBLIC,",
    "PERSONAL_ONLY,NOT_SCENERY,PERSONAL_ONLY,NOT_SCENERY",
    "PERSONAL_ONLY,PRIVACY,PERSONAL_ONLY,PRIVACY",
    "PERSONAL_ONLY,NONE,PERSONAL_ONLY,NOT_SCENERY",
    "REJECT,INAPPROPRIATE,REJECT,INAPPROPRIATE"
  })
  void judgementMapsToResult(Judgement.Verdict verdict, Judgement.Reason reason,
      Decision decision, ModerationResult.Reason expectedReason) {
    var result = ClaudeImageModerator.toResult(new Judgement(verdict, reason));
    assertEquals(decision, result.decision());
    assertEquals(expectedReason, result.reason());
  }

  @Test
  void missingVerdictIsUnchecked() {
    assertEquals(Decision.UNCHECKED,
        ClaudeImageModerator.toResult(new Judgement(null, null)).decision());
  }

  @Test
  @SuppressWarnings("unchecked")
  void refusalIsRejected() {
    StructuredMessage<Judgement> response = mock(StructuredMessage.class);
    when(response.stopReason()).thenReturn(Optional.of(StopReason.REFUSAL));
    assertTrue(ClaudeImageModerator.toResult(response).isRejected());
  }

  @Test
  @SuppressWarnings("unchecked")
  void emptyContentIsUnchecked() {
    StructuredMessage<Judgement> response = mock(StructuredMessage.class);
    when(response.stopReason()).thenReturn(Optional.of(StopReason.END_TURN));
    when(response.content()).thenReturn(List.of());
    assertEquals(Decision.UNCHECKED, ClaudeImageModerator.toResult(response).decision());
  }

  @Test
  void apiFailureIsUncheckedInsteadOfThrowing() {
    AnthropicClient client = mock(AnthropicClient.class, RETURNS_DEEP_STUBS);
    when(client.messages().create(
        any(StructuredMessageCreateParams.class)))
        .thenThrow(new AnthropicIoException("timeout"));
    var moderator = new ClaudeImageModerator(client, "claude-haiku-5-5");
    assertEquals(Decision.UNCHECKED, moderator.moderate(new byte[] {1, 2, 3}).decision());
  }

  @Test
  void startupAccessCheckFailureDoesNotStopServer() {
    AnthropicClient client = mock(AnthropicClient.class, RETURNS_DEEP_STUBS);
    when(client.models().retrieve("claude-haiku-5-5"))
        .thenThrow(new AnthropicIoException("unreachable"));
    var moderator = new ClaudeImageModerator(client, "claude-haiku-5-5");
    assertDoesNotThrow(moderator::verifyAccess);
  }
}
