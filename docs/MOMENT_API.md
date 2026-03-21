# Moment System API Documentation

## Part 5: Moment System

### Database Setup

Run the SQL script to create tables:
```bash
mysql -u root -p byteferry < sql/moment_tables.sql
```

### API Endpoints

#### 1. Create Moment
```http
POST /api/moment
Authorization: Bearer {token}
Content-Type: multipart/form-data

Parameters:
- textContent: String (optional) - Plain text content
- htmlContent: String (optional) - Custom HTML content
- templateId: String (optional) - Template ID (card, magazine, minimal, polaroid, gradient)
- visibility: String (default: PUBLIC) - PUBLIC, PRIVATE, VISIBLE_TO, HIDDEN_FROM
- visibleUserIds: String (optional) - Comma-separated user IDs for VISIBLE_TO/HIDDEN_FROM
- images: MultipartFile[] (optional) - Regular images (max 9)
- liveImages: MultipartFile[] (optional) - Live Photo static images
- liveVideos: MultipartFile[] (optional) - Live Photo videos (must match liveImages count)

Response: Moment object with images
```

#### 2. Get Single Moment
```http
GET /api/moment/{id}
Authorization: Bearer {token}

Response: Moment object (if viewer has permission)
```

#### 3. Get My Moments
```http
GET /api/moment/my?page=0&size=10
Authorization: Bearer {token}

Response: Page<Moment>
```

#### 4. Get User's Moments
```http
GET /api/moment/user/{username}?page=0&size=10
Authorization: Bearer {token}

Response: Page<Moment> (filtered by visibility rules)
```

#### 5. Update Moment
```http
PUT /api/moment/{id}
Authorization: Bearer {token}
Content-Type: multipart/form-data

Parameters:
- textContent: String (optional)
- htmlContent: String (optional)
- visibility: String (optional)
- visibleUserIds: String (optional)

Response: Updated Moment object
```

#### 6. Delete Moment
```http
DELETE /api/moment/{id}
Authorization: Bearer {token}

Response: 200 OK (also deletes files from MinIO)
```

#### 7. Get All Templates
```http
GET /api/moment/templates
Authorization: Bearer {token}

Response: List<MomentTemplate>
```

#### 8. Get Template Details
```http
GET /api/moment/templates/{id}
Authorization: Bearer {token}

Response: MomentTemplate object
```

---

## Part 6: Moment Share Link

### API Endpoints

#### 1. Generate Share Link
```http
POST /api/moment/share/generate
Authorization: Bearer {token}

Response:
{
  "id": 1,
  "userId": 123,
  "shareCode": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "createdAt": "2026-03-21T23:00:00"
}
```

#### 2. Get My Share Link
```http
GET /api/moment/share/my
Authorization: Bearer {token}

Response: MomentShareLink object
```

#### 3. View Moments via Share Link
```http
GET /api/moment/share/{shareCode}?page=0&size=10
Authorization: Bearer {token}

Response: Page<Moment> (filtered by visibility rules relative to viewer)
```

---

## Visibility Rules

### PUBLIC
- Everyone can see (including non-friends)

### PRIVATE
- Only the owner can see

### VISIBLE_TO
- Only specified users can see
- Requires `visibleUserIds` parameter

### HIDDEN_FROM
- Everyone except specified users can see
- Requires `visibleUserIds` parameter

---

## Testing Examples

### 1. Create a public moment with text
```bash
curl -X POST http://localhost:8080/api/moment \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "textContent=Hello World!" \
  -F "visibility=PUBLIC"
```

### 2. Create a moment with images
```bash
curl -X POST http://localhost:8080/api/moment \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "textContent=Check out these photos" \
  -F "visibility=PUBLIC" \
  -F "images=@photo1.jpg" \
  -F "images=@photo2.jpg"
```

### 3. Create a moment with Live Photo
```bash
curl -X POST http://localhost:8080/api/moment \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "textContent=My Live Photo" \
  -F "visibility=PUBLIC" \
  -F "liveImages=@photo.jpg" \
  -F "liveVideos=@photo.mov"
```

### 4. Create a moment with template
```bash
curl -X POST http://localhost:8080/api/moment \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "textContent=Beautiful sunset" \
  -F "templateId=polaroid" \
  -F "visibility=PUBLIC"
```

### 5. Create a moment visible only to specific users
```bash
curl -X POST http://localhost:8080/api/moment \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "textContent=Private message" \
  -F "visibility=VISIBLE_TO" \
  -F "visibleUserIds=2,3,5"
```

### 6. Generate share link
```bash
curl -X POST http://localhost:8080/api/moment/share/generate \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 7. View moments via share link
```bash
curl -X GET http://localhost:8080/api/moment/share/a1b2c3d4e5f6g7h8 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## Notes

1. **Image Limits**: Maximum 9 images per moment (including Live Photos)
2. **File Size Limits**:
   - Images: 10MB
   - Videos (Live Photo): 50MB
3. **Supported Formats**:
   - Images: JPG, JPEG, PNG, GIF, WEBP
   - Videos: MP4, MOV
4. **Templates**: 5 built-in templates (card, magazine, minimal, polaroid, gradient)
5. **Share Links**: Each user can have only one share link (regenerating replaces the old one)
6. **Visibility**: Share link viewers must be logged in and respect visibility rules
