-- Serves: a circle's activity feed — "the most recent check-ins across these
-- habits", i.e. WHERE habit_id IN (...) ORDER BY recorded_at DESC LIMIT n.
CREATE INDEX idx_checkins_habit_recorded_at ON checkins (habit_id, recorded_at DESC);
