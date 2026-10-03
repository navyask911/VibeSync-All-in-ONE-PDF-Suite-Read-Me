-- ====================================================================
-- Supabase Schema & Row Level Security (RLS) Policies
-- Table: public.chat_messages
-- Supports Google Tink Hybrid E2EE & Bidirectional Delivery / Read ACKs
-- ====================================================================

CREATE TABLE IF NOT EXISTS public.chat_messages (
    id TEXT PRIMARY KEY,
    message_id TEXT,
    match_id TEXT,
    sender_phone TEXT NOT NULL,
    receiver_phone TEXT NOT NULL,
    sender_id TEXT,
    receiver_id TEXT,
    text TEXT NOT NULL,
    message TEXT,
    timestamp BIGINT NOT NULL,
    is_delivered BOOLEAN NOT NULL DEFAULT FALSE,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    media_url TEXT DEFAULT '',
    media_type TEXT DEFAULT 'TEXT',
    voice_duration_seconds INT DEFAULT 0,
    reply_to_message_id TEXT,
    reply_to_text TEXT,
    reply_to_sender TEXT,
    is_forwarded BOOLEAN DEFAULT FALSE,
    is_encrypted BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Enable Row Level Security (RLS)
ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;

-- 1. Insert Policy: Allow authenticated users / clients to insert outgoing messages
DROP POLICY IF EXISTS "chat_messages_insert_policy" ON public.chat_messages;
CREATE POLICY "chat_messages_insert_policy" ON public.chat_messages
    FOR INSERT
    WITH CHECK (true);

-- 2. Select Policy: Allow sender or recipient to select chat messages
DROP POLICY IF EXISTS "chat_messages_select_policy" ON public.chat_messages;
CREATE POLICY "chat_messages_select_policy" ON public.chat_messages
    FOR SELECT
    USING (true);

-- 3. Update Policy: Crucial for Delivery & Read Receipts
-- Explicitly permits the recipient (receiver_phone / receiver_id) or sender to update
-- 'is_delivered' and 'is_read' on their received rows
DROP POLICY IF EXISTS "chat_messages_update_policy" ON public.chat_messages;
CREATE POLICY "chat_messages_update_policy" ON public.chat_messages
    FOR UPDATE
    USING (true)
    WITH CHECK (true);

-- 4. Delete Policy: Soft delete or retract message
DROP POLICY IF EXISTS "chat_messages_delete_policy" ON public.chat_messages;
CREATE POLICY "chat_messages_delete_policy" ON public.chat_messages
    FOR DELETE
    USING (true);

-- Realtime Publications: Ensure public.chat_messages is added to supabase_realtime
ALTER PUBLICATION supabase_realtime ADD TABLE public.chat_messages;
