# ByteFerry - Feature Tracking

> `#` = implemented

---

## Product Positioning

- **Token mode**: Share code for quick transfer, no login required (logged-in users can also use)
- **Multi-device mode**: Same account shares content across devices
- **Friend session mode**: Friends can open bidirectional transfer sessions

---

## MVP Version (Completed)

- [x] # Text sharing (paste & send)
- [x] # Image upload & sharing
- [x] # File upload & sharing (max 100MB)
- [x] # Batch file/image upload
- [x] # Generate random share code (6-char, A-Z + 0-9)
- [x] # Retrieve content via share code
- [x] # Redis TTL auto-expiry (default 10 min)
- [x] # Delete after download option
- [x] # Drag & drop upload (image / file)
- [x] # One-click copy (text content / share code)
- [x] # File download & image preview
- [x] # Manual delete share
- [x] # Responsive web UI

---

## Phase 2 - Token Session Mode

> One token opens a persistent session, supports sending multiple items within the session

- [x] # Token session: one code creates a session, sender can push multiple text/image/file into it
- [x] # Receiver opens session page, sees all items in real-time (WebSocket / polling)
- [x] # Session expiry time (configurable, default 30 min)
- [x] # Session auto-cleanup: close session deletes all files and Redis data
- [x] # Session status indicator (active / expired)
- [x] # Custom expiry time selection in UI

---

## Phase 3 - User System & Multi-Device Sharing

> Login-based features, requires MySQL

- [x] # User registration & login (username + password)
- [x] # JWT token authentication
- [x] # MySQL database for user data
- [x] # Personal shared space: logged-in user has a dedicated page showing all their shared content
- [x] # Multi-device sync: same account on multiple devices sees the same shared space
- [x] # Shared space content management (delete, view history)
- [x] # Space item expiration: each item has independent expiry time (10min/30min/1h/2h), auto-cleanup on expire
- [x] # Space WebSocket real-time push: instant sync across devices, auto-disconnect when all items expire
- [x] # Space Clear All: one-click delete all items

---

## Phase 4 - Friend System & Bidirectional Sessions

> Social features for logged-in users

- [x] # Add friend (by username)
- [x] # Friend list management (add / remove / block)
- [x] # Friend request & accept flow
- [x] # Open session with friend: create a bidirectional transfer channel
- [x] # Bidirectional session: both sides can send text / image / file
- [x] # In-session actions: copy text, preview image, download file
- [x] # Session expiry: configurable timeout (10min/30min/1h/2h), auto-close when expired
- [x] # Session close: auto-cleanup all transferred resources (files on disk + Redis data)
- [x] # Session history (list of past sessions, no content retained after close)
- [x] # Online status indicator for friends (WebSocket-based)

---

## Phase 4.5 - Multi-User Sessions & Invitation System

> Upgrade friend sessions from 1-on-1 to multi-user with invitation flow

- [x] # Multi-participant session data model (FriendSessionData with Participant list, admin role)
- [x] # Invitation system: send/accept/decline session invitations
- [x] # Invite to existing session (add more friends mid-session)
- [x] # Pending invitations list with real-time WebSocket notifications
- [x] # Session re-entry: leave session view without closing, re-enter from active sessions list
- [x] # Active sessions list on Friends tab
- [x] # Multi-user session management: kick member, leave session, admin-only close
- [x] # Session members panel (view participants, admin badge, kick button)
- [x] # Per-user message color coding (8 distinct colors for sender identification)
- [x] # Single-device WebSocket enforcement (new connection replaces old)
- [x] # Friend search/filter in friend list (client-side, with online-first sorting)
- [x] # Multi-user session history (per-participant records with participant names)
- [x] # Global invite permission toggle (admin controls whether members can invite)
- [x] # Per-member invite permission toggle

---

## Phase 5 - Enhancement & Polish

- [ ] QR code generation on share
- [ ] Clipboard auto-detect (navigator.clipboard on page load)
- [ ] Image paste from clipboard (Ctrl+V)
- [ ] URL link preview (auto-detect URL, show title/preview)
- [ ] Code syntax highlighting
- [ ] Client-side AES encryption
- [ ] Browser extension (auto-sync on Ctrl+C)
- [ ] CLI tool (`share "hello world"`)
- [ ] Open API with authentication
- [ ] PWA support (installable, offline)
- [ ] Docker + Nginx deployment config
