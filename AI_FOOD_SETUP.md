# Activate AI Food Photo Analysis
Ascension's Progress > Camera Food Log now supports AI photo estimates. Photos are sent only when the user taps **Analyze Photo with AI**. The app resizes photos before upload, presents editable estimates, and never automatically saves an AI estimate.

## Required deployment (not configured by repository commits)
1. Create or select a Supabase project and run guild-schema.sql in SQL Editor to enable authenticated Guild accounts.
2. Configure email OTP sign-in under Supabase Authentication and sign in from the app's Guild tab.
3. Set the secret OPENAI_API_KEY on the Supabase project (Supabase dashboard Edge Functions > Secrets, or Supabase CLI).
4. Deploy supabase/functions/analyze-food/index.ts as the Supabase Edge Function named analyze-food. Leave JWT verification enabled.
5. In the app's Guild tab, enter the Supabase project URL and its publishable/anon key. **Never enter an OpenAI API key or Supabase service-role key into the app.**
6. Take a photo in Progress > Camera Food Log, tap Analyze Photo with AI, check portion and numbers, then Add to Today's Macros.

This uses OpenAI Responses API gpt-4.1-mini with a strict nutrition JSON schema. There are per-image API costs. Nutritional estimates from food photos are inherently approximate; cooking oils, hidden ingredients, portion sizes, and labels with per-100g values can cause errors. Missing values are deliberately left blank for manual entry.

Privacy: The photo is sent to your Supabase Edge Function and OpenAI for processing. The Edge Function does not persist the image and uses store:false. This is not a local-only photo analysis feature. Consult your own data retention and privacy policy before publishing.

Production recommendations: add per-user rate limiting and cost budgets, monitor errors, secure auth refresh, validate food-entry totals server-side if introducing rewards, and consider a verified food database for better accuracy.