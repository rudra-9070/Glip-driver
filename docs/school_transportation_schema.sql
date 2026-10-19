-- SQL Schema for School Transportation Service
-- Run this script in your Supabase SQL Editor:
-- Dashboard -> Select Project (uugzolriwmbhssgfjbap) -> SQL Editor -> New Query -> Run

CREATE TABLE IF NOT EXISTS public.school_transportation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_name TEXT NOT NULL,
    driver_mobile TEXT NOT NULL,
    vehicle_number TEXT,
    home_address TEXT NOT NULL,
    home_lat DOUBLE PRECISION,
    home_lng DOUBLE PRECISION,
    status TEXT DEFAULT 'REGISTERED',
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Enable Row Level Security (RLS)
ALTER TABLE public.school_transportation ENABLE ROW LEVEL SECURITY;

-- Policy to allow inserting school transportation registrations via anon API key
CREATE POLICY "Allow public insert to school_transportation"
ON public.school_transportation
FOR INSERT
TO anon, authenticated
WITH CHECK (true);

-- Policy to allow selecting school transportation registrations via anon API key
CREATE POLICY "Allow public select from school_transportation"
ON public.school_transportation
FOR SELECT
TO anon, authenticated
USING (true);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_school_transportation_mobile ON public.school_transportation (driver_mobile);
CREATE INDEX IF NOT EXISTS idx_school_transportation_created_at ON public.school_transportation (created_at DESC);
