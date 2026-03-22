-- Remove template system and add card mode

-- Drop template table
DROP TABLE IF EXISTS moment_templates;

-- Modify moments table
ALTER TABLE moments
  DROP COLUMN IF EXISTS html_content,
  DROP COLUMN IF EXISTS template_id,
  ADD COLUMN IF NOT EXISTS card_mode TINYINT(1) DEFAULT 0 AFTER text_content;
