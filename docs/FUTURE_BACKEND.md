# Future Backend & Supabase Integration Plan

This document details how the Glip Driver MVP will be connected to a production Supabase backend when moving beyond local demo mode.

---

## 1. Supabase Database Schema

### A. `driver_profiles`
```sql
create table driver_profiles (
  id uuid references auth.users not null primary key,
  full_name text not null,
  mobile_number text not null unique,
  email text,
  status text not null default 'pending_verification', -- pending_verification, verified, suspended
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

alter table driver_profiles enable row level security;

-- Policy: Drivers can view and update their own profile only
create policy "Drivers can view own profile" 
  on driver_profiles for select 
  using (auth.uid() = id);

create policy "Drivers can update own profile" 
  on driver_profiles for update 
  using (auth.uid() = id);
```

### B. `vehicles`
```sql
create table vehicles (
  id uuid default gen_random_uuid() primary key,
  driver_id uuid references driver_profiles(id) on delete cascade not null,
  display_name text not null,
  category text not null,
  registration_number text not null,
  total_capacity int not null check (total_capacity > 0),
  occupied_seats int not null default 0 check (occupied_seats >= 0),
  created_at timestamp with time zone default now()
);

alter table vehicles enable row level security;
create policy "Drivers manage own vehicles" 
  on vehicles for all 
  using (auth.uid() = driver_id);
```

### C. `driver_routes`
```sql
create table driver_routes (
  id uuid default gen_random_uuid() primary key,
  driver_id uuid references driver_profiles(id) on delete cascade not null,
  origin_name text not null,
  destination_name text not null,
  departure_time text not null,
  direction text not null,
  is_active boolean default true,
  stops jsonb default '[]'::jsonb
);
```

### D. `passenger_pool_requests`
```sql
create table passenger_pool_requests (
  id uuid default gen_random_uuid() primary key,
  passenger_id uuid not null,
  pickup_name text not null,
  dropoff_name text not null,
  requested_seats int not null,
  illustrative_fare numeric(10, 2),
  status text not null default 'pending' -- pending, accepted, rejected, completed
);
```

---

## 2. API Key Security & Client Rules

1. **Never embed the `service_role` secret in the Android APK**: The mobile client should only ever contain the `anon` public publishable key.
2. **Row Level Security (RLS)**: Enforce strict database policies so drivers cannot see other drivers' private KYC documents, bank accounts, or passenger phone numbers before mutual match confirmation.
3. **Route Matching in Edge Functions / Postgres PostGIS**: In production, route matching should be performed server-side using Postgres PostGIS (`ST_DWithin` and polyline buffer corridors) so that only compatible commuter requests are dispatched to the driver.
