-- ====================================================================
-- VibeMeet — Casablanca Discovery App
-- Complete schema migration: tables, RLS, triggers, indexes, seed data
-- ====================================================================

-- 1) PROFILES TABLE -- extend the existing one (created by handle_new_user)
-- ====================================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    pseudo TEXT,
    email TEXT,
    avatar_url TEXT,
    mood TEXT DEFAULT 'happy',
    interests TEXT[] DEFAULT ARRAY[]::TEXT[],
    budget_preference INT DEFAULT 2,
    onboarding_completed BOOLEAN DEFAULT FALSE,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    bio TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.profiles
    ADD COLUMN IF NOT EXISTS avatar_url TEXT,
    ADD COLUMN IF NOT EXISTS mood TEXT DEFAULT 'happy',
    ADD COLUMN IF NOT EXISTS interests TEXT[] DEFAULT ARRAY[]::TEXT[],
    ADD COLUMN IF NOT EXISTS budget_preference INT DEFAULT 2,
    ADD COLUMN IF NOT EXISTS onboarding_completed BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS bio TEXT,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- 2) VENUES TABLE
-- ====================================================================
CREATE TABLE IF NOT EXISTS public.venues (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    description TEXT,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    address TEXT,
    district TEXT,
    rating REAL DEFAULT 0,
    review_count INT DEFAULT 0,
    image_emoji TEXT,
    tags TEXT[] DEFAULT ARRAY[]::TEXT[],
    vibe_keywords TEXT[] DEFAULT ARRAY[]::TEXT[],
    price_level INT DEFAULT 2 CHECK (price_level BETWEEN 1 AND 3),
    open_hour INT DEFAULT 0 CHECK (open_hour BETWEEN 0 AND 24),
    close_hour INT DEFAULT 24 CHECK (close_hour BETWEEN 0 AND 24),
    phone_number TEXT,
    website TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3) FAVORITES TABLE
-- ====================================================================
CREATE TABLE IF NOT EXISTS public.favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    venue_id TEXT NOT NULL REFERENCES public.venues(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, venue_id)
);

-- 4) VISITS / CHECK-INS TABLE
-- ====================================================================
CREATE TABLE IF NOT EXISTS public.visits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    venue_id TEXT NOT NULL REFERENCES public.venues(id) ON DELETE CASCADE,
    venue_name TEXT,
    venue_type TEXT,
    notes TEXT,
    visited_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5) ITINERARIES TABLE (AI-generated day plans)
-- ====================================================================
CREATE TABLE IF NOT EXISTS public.itineraries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT,
    user_query TEXT,
    content TEXT,
    venue_ids TEXT[] DEFAULT ARRAY[]::TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6) CHAT MESSAGES TABLE (AI conversation history)
-- ====================================================================
CREATE TABLE IF NOT EXISTS public.chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_user_message BOOLEAN NOT NULL DEFAULT TRUE,
    session_id UUID,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7) REVIEWS TABLE (users can rate visited venues)
-- ====================================================================
CREATE TABLE IF NOT EXISTS public.reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    venue_id TEXT NOT NULL REFERENCES public.venues(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, venue_id)
);

-- ====================================================================
-- INDEXES for performance
-- ====================================================================
CREATE INDEX IF NOT EXISTS idx_profiles_pseudo ON public.profiles(pseudo);
CREATE INDEX IF NOT EXISTS idx_favorites_user ON public.favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_favorites_venue ON public.favorites(venue_id);
CREATE INDEX IF NOT EXISTS idx_visits_user ON public.visits(user_id);
CREATE INDEX IF NOT EXISTS idx_visits_venue ON public.visits(venue_id);
CREATE INDEX IF NOT EXISTS idx_visits_user_date ON public.visits(user_id, visited_at DESC);
CREATE INDEX IF NOT EXISTS idx_venues_type ON public.venues(type);
CREATE INDEX IF NOT EXISTS idx_venues_district ON public.venues(district);
CREATE INDEX IF NOT EXISTS idx_venues_location ON public.venues(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_itineraries_user ON public.itineraries(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_user ON public.chat_messages(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON public.chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_reviews_venue ON public.reviews(venue_id);

-- ====================================================================
-- TRIGGER: keep profiles.updated_at fresh
-- ====================================================================
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS profiles_updated_at ON public.profiles;
CREATE TRIGGER profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- ====================================================================
-- TRIGGER: auto-create profile when user signs up
-- ====================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.profiles (id, email, pseudo)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'pseudo', split_part(NEW.email, '@', 1))
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ====================================================================
-- ROW LEVEL SECURITY
-- ====================================================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.venues ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.favorites ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.visits ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.itineraries ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;

-- Profiles: everyone can read, only owner can update
DROP POLICY IF EXISTS "Profiles are viewable by everyone" ON public.profiles;
CREATE POLICY "Profiles are viewable by everyone"
    ON public.profiles FOR SELECT TO authenticated, anon USING (true);

DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Users can insert own profile"
    ON public.profiles FOR INSERT TO authenticated WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE TO authenticated USING (auth.uid() = id);

-- Venues: everyone reads, only authenticated users can suggest (typical seed model)
DROP POLICY IF EXISTS "Venues are public" ON public.venues;
CREATE POLICY "Venues are public"
    ON public.venues FOR SELECT TO authenticated, anon USING (true);

DROP POLICY IF EXISTS "Auth users can insert venues" ON public.venues;
CREATE POLICY "Auth users can insert venues"
    ON public.venues FOR INSERT TO authenticated WITH CHECK (true);

-- Favorites
DROP POLICY IF EXISTS "Users see own favorites" ON public.favorites;
CREATE POLICY "Users see own favorites"
    ON public.favorites FOR SELECT TO authenticated USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users insert own favorites" ON public.favorites;
CREATE POLICY "Users insert own favorites"
    ON public.favorites FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users delete own favorites" ON public.favorites;
CREATE POLICY "Users delete own favorites"
    ON public.favorites FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- Visits
DROP POLICY IF EXISTS "Users see own visits" ON public.visits;
CREATE POLICY "Users see own visits"
    ON public.visits FOR SELECT TO authenticated USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users manage own visits" ON public.visits;
CREATE POLICY "Users manage own visits"
    ON public.visits FOR ALL TO authenticated
    USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- Itineraries
DROP POLICY IF EXISTS "Users manage own itineraries" ON public.itineraries;
CREATE POLICY "Users manage own itineraries"
    ON public.itineraries FOR ALL TO authenticated
    USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- Chat messages
DROP POLICY IF EXISTS "Users manage own chat" ON public.chat_messages;
CREATE POLICY "Users manage own chat"
    ON public.chat_messages FOR ALL TO authenticated
    USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- Reviews
DROP POLICY IF EXISTS "Reviews are public" ON public.reviews;
CREATE POLICY "Reviews are public"
    ON public.reviews FOR SELECT TO authenticated, anon USING (true);

DROP POLICY IF EXISTS "Users manage own reviews" ON public.reviews;
CREATE POLICY "Users manage own reviews"
    ON public.reviews FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users update own reviews" ON public.reviews;
CREATE POLICY "Users update own reviews"
    ON public.reviews FOR UPDATE TO authenticated USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users delete own reviews" ON public.reviews;
CREATE POLICY "Users delete own reviews"
    ON public.reviews FOR DELETE TO authenticated USING (auth.uid() = user_id);
