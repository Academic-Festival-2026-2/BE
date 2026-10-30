-- Run against the intended BINTEUM database to remove only the old product tables.
-- User accounts and refresh tokens are intentionally preserved.
DROP TABLE IF EXISTS participation;
DROP TABLE IF EXISTS feedback;
DROP TABLE IF EXISTS class_schedule;
DROP TABLE IF EXISTS study;
DROP TABLE IF EXISTS classroom;
DROP TABLE IF EXISTS building;
