-- Run in Supabase SQL Editor before enabling Ascension Guild.
-- Auth uses email OTP. Configure Email OTP in Supabase Auth.
create table if not exists public.ascension_profiles (
 user_id uuid primary key references auth.users(id) on delete cascade,
 display_name text not null check (char_length(display_name) between 1 and 30),
 xp integer not null default 0 check (xp >= 0),
 sessions integer not null default 0 check (sessions >= 0),
 training_class text not null default 'warden',
 updated_at timestamptz not null default now()
);
create table if not exists public.ascension_friendships (
 id bigint generated always as identity primary key,
 requester_id uuid not null references auth.users(id) on delete cascade,
 recipient_id uuid not null references auth.users(id) on delete cascade,
 status text not null default 'pending' check (status in ('pending','accepted')),
 created_at timestamptz not null default now(),
 unique(requester_id,recipient_id),
 check(requester_id <> recipient_id)
);
create index if not exists ascension_friendships_recipient_idx on public.ascension_friendships(recipient_id);
alter table public.ascension_profiles enable row level security;
alter table public.ascension_friendships enable row level security;
revoke all on public.ascension_profiles from anon;
revoke all on public.ascension_friendships from anon;
grant select,insert,update on public.ascension_profiles to authenticated;
grant select,update on public.ascension_friendships to authenticated;
create policy "owner can read own profile" on public.ascension_profiles for select to authenticated using (user_id=auth.uid());
create policy "owner can insert profile" on public.ascension_profiles for insert to authenticated with check (user_id=auth.uid());
create policy "owner can update profile" on public.ascension_profiles for update to authenticated using (user_id=auth.uid()) with check (user_id=auth.uid());
create policy "friendship members read" on public.ascension_friendships for select to authenticated using (requester_id=auth.uid() or recipient_id=auth.uid());
create policy "recipient accepts" on public.ascension_friendships for update to authenticated using (recipient_id=auth.uid() and status='pending') with check (recipient_id=auth.uid() and status='accepted');
-- Invites by email are created on the server, not from client-submitted UUIDs.
create or replace function public.ascension_invite_friend(friend_email text)
returns void language plpgsql security definer set search_path = ''
as $$
declare target uuid;
begin
 if auth.uid() is null then raise exception 'Sign in first'; end if;
 select id into target from auth.users where lower(email)=lower(trim(friend_email)) limit 1;
 if target is null then raise exception 'Friend must sign up before you can invite them'; end if;
 if target=auth.uid() then raise exception 'Cannot invite yourself'; end if;
 if exists(select 1 from public.ascension_friendships f where
 (f.requester_id=auth.uid() and f.recipient_id=target) or
 (f.requester_id=target and f.recipient_id=auth.uid())) then
 raise exception 'Friendship or invite already exists'; end if;
 insert into public.ascension_friendships(requester_id,recipient_id) values(auth.uid(),target);
end; $$;
revoke all on function public.ascension_invite_friend(text) from public,anon;
grant execute on function public.ascension_invite_friend(text) to authenticated;
-- SECURITY INVOKER: access only own profile and accepted friends.
create or replace view public.ascension_leaderboard with (security_invoker=true) as
 select p.user_id,p.display_name,p.xp,p.sessions,p.training_class
 from public.ascension_profiles p
 where p.user_id=auth.uid()
 or exists (select 1 from public.ascension_friendships f
 where f.status='accepted' and
 ((f.requester_id=auth.uid() and f.recipient_id=p.user_id) or
 (f.recipient_id=auth.uid() and f.requester_id=p.user_id)));
-- Profile RLS must permit accepted friends to read profile rows.
drop policy if exists "accepted friends can read profile" on public.ascension_profiles;
create policy "accepted friends can read profile" on public.ascension_profiles for select to authenticated using (
 exists(select 1 from public.ascension_friendships f where f.status='accepted'
 and ((f.requester_id=auth.uid() and f.recipient_id=user_id)
 or (f.recipient_id=auth.uid() and f.requester_id=user_id)))
);
grant select on public.ascension_leaderboard to authenticated;
-- IMPORTANT: XP originates on device and can be tampered with.
-- For verified competitive leaderboards, use server-validated signed workout events.
