-- Part 5: Moment System Tables

-- Moments table
CREATE TABLE moments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    text_content TEXT,
    html_content MEDIUMTEXT,
    template_id VARCHAR(50),
    visibility ENUM('PUBLIC', 'PRIVATE', 'VISIBLE_TO', 'HIDDEN_FROM') DEFAULT 'PUBLIC',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Moment images table (max 9 images per moment)
CREATE TABLE moment_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    moment_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    video_url VARCHAR(500),
    is_live_photo TINYINT(1) DEFAULT 0,
    sort_order INT DEFAULT 0,
    FOREIGN KEY (moment_id) REFERENCES moments(id) ON DELETE CASCADE,
    INDEX idx_moment_id (moment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Moment visibility rules table
CREATE TABLE moment_visibility_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    moment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (moment_id) REFERENCES moments(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_moment_user (moment_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Moment templates table
CREATE TABLE moment_templates (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    html_template MEDIUMTEXT NOT NULL,
    preview_image VARCHAR(500),
    sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default templates
INSERT INTO moment_templates (id, name, description, html_template, sort_order) VALUES
('card', 'Card Style', 'Rounded corners, shadow, centered layout',
'<div style="background:#fff;border-radius:12px;box-shadow:0 4px 12px rgba(0,0,0,0.1);padding:24px;max-width:600px;margin:0 auto;"><div style="font-size:18px;line-height:1.6;color:#333;">{{content}}</div></div>', 1),

('magazine', 'Magazine Style', 'Large image with text overlay',
'<div style="position:relative;max-width:800px;margin:0 auto;"><div style="background:linear-gradient(to bottom,rgba(0,0,0,0.3),rgba(0,0,0,0.7));padding:40px;color:#fff;"><div style="font-size:24px;font-weight:bold;margin-bottom:16px;">{{title}}</div><div style="font-size:16px;line-height:1.8;">{{content}}</div></div></div>', 2),

('minimal', 'Minimal Style', 'Lots of whitespace, serif font',
'<div style="max-width:700px;margin:60px auto;padding:40px;font-family:Georgia,serif;"><div style="font-size:20px;line-height:2;color:#444;text-align:justify;">{{content}}</div></div>', 3),

('polaroid', 'Polaroid Style', 'Photo frame with handwritten text',
'<div style="background:#fff;padding:16px 16px 60px;box-shadow:0 8px 16px rgba(0,0,0,0.15);max-width:400px;margin:0 auto;transform:rotate(-2deg);"><div style="background:#f5f5f5;padding:20px;min-height:200px;display:flex;align-items:center;justify-content:center;"><div style="font-family:\'Comic Sans MS\',cursive;font-size:16px;color:#555;text-align:center;">{{content}}</div></div></div>', 4),

('gradient', 'Gradient Style', 'Gradient background with white text',
'<div style="background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:60px 40px;border-radius:16px;max-width:700px;margin:0 auto;"><div style="font-size:20px;line-height:1.8;color:#fff;text-align:center;">{{content}}</div></div>', 5);

-- Part 6: Moment Share Link Table
CREATE TABLE moment_share_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    share_code VARCHAR(32) NOT NULL UNIQUE,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_share_code (share_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
