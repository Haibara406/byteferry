// ==================== Constants ====================
const API_BASE = '/api';
const TOKEN_KEY = 'bf_token';

// ==================== DOM Elements ====================
const backHomeBtn = document.getElementById('back-home-btn');
const profileAvatar = document.getElementById('profile-avatar');
const profileAvatarWrap = document.getElementById('profile-avatar-wrap');
const avatarInput = document.getElementById('avatar-input');
const profileUsernameDisplay = document.getElementById('profile-username-display');
const profileEmailDisplay = document.getElementById('profile-email-display');
const profileCreatedDisplay = document.getElementById('profile-created-display');
const editUsername = document.getElementById('edit-username');
const genderNav = document.getElementById('gender-nav');
const saveProfileBtn = document.getElementById('save-profile-btn');
const profileSuccess = document.getElementById('profile-success');
const profileError = document.getElementById('profile-error');
const currentEmailDisplay = document.getElementById('current-email-display');
const newEmail = document.getElementById('new-email');
const emailCode = document.getElementById('email-code');
const sendEmailCodeBtn = document.getElementById('send-email-code-btn');
const changeEmailBtn = document.getElementById('change-email-btn');
const emailSuccess = document.getElementById('email-success');
const emailError = document.getElementById('email-error');
const toast = document.getElementById('toast');

let currentUser = null;
let selectedGender = 'UNKNOWN';

// ==================== Utility Functions ====================
function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

function showToast(message, type = 'normal') {
    toast.textContent = message;
    toast.className = `toast ${type} show`;
    setTimeout(() => toast.classList.remove('show'), 3000);
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

async function apiCall(endpoint, options = {}) {
    const token = getToken();
    const headers = {
        ...options.headers,
    };
    if (token && !options.skipAuth) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    if (!(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }

    const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        headers,
    });

    if (!response.ok) {
        const error = await response.text();
        throw new Error(error || 'Request failed');
    }

    return response.json();
}

// ==================== Load Profile ====================
async function loadProfile() {
    try {
        const data = await apiCall('/auth/me');
        currentUser = data;

        profileAvatar.src = data.avatar || '/images/default-avatar.jpg';
        profileUsernameDisplay.textContent = data.username;
        profileEmailDisplay.textContent = data.email || 'Not set';
        profileCreatedDisplay.textContent = `Joined ${formatDate(data.createdAt)}`;

        editUsername.value = data.username;
        currentEmailDisplay.value = data.email || '';

        selectedGender = data.gender || 'UNKNOWN';
        updateGenderUI();
    } catch (error) {
        showToast('Failed to load profile', 'error');
        console.error(error);
    }
}

function updateGenderUI() {
    genderNav.querySelectorAll('.pill').forEach(btn => {
        if (btn.dataset.gender === selectedGender) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });
}

// ==================== Avatar Upload ====================
profileAvatarWrap.addEventListener('click', () => {
    avatarInput.click();
});

avatarInput.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Validate file type
    const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    if (!validTypes.includes(file.type)) {
        showToast('Please select a valid image (JPG, PNG, WEBP)', 'error');
        return;
    }

    // Validate file size (5MB)
    if (file.size > 5 * 1024 * 1024) {
        showToast('Image size must be less than 5MB', 'error');
        return;
    }

    try {
        const formData = new FormData();
        formData.append('file', file);

        const token = getToken();
        const response = await fetch(`${API_BASE}/user/avatar`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
            },
            body: formData,
        });

        if (!response.ok) {
            throw new Error('Upload failed');
        }

        const data = await response.json();
        profileAvatar.src = data.avatar;
        showToast('Avatar updated successfully', 'success');

        // Reload to update header avatar
        setTimeout(() => location.reload(), 1500);
    } catch (error) {
        showToast('Failed to upload avatar', 'error');
        console.error(error);
    }
});

// ==================== Gender Selection ====================
genderNav.addEventListener('click', (e) => {
    const btn = e.target.closest('.pill');
    if (!btn) return;

    selectedGender = btn.dataset.gender;
    updateGenderUI();
});

// ==================== Save Profile ====================
saveProfileBtn.addEventListener('click', async () => {
    const username = editUsername.value.trim();

    if (!username) {
        profileError.textContent = 'Username is required';
        profileError.classList.remove('hidden');
        profileSuccess.classList.add('hidden');
        return;
    }

    if (username.length < 3 || username.length > 50) {
        profileError.textContent = 'Username must be 3-50 characters';
        profileError.classList.remove('hidden');
        profileSuccess.classList.add('hidden');
        return;
    }

    try {
        await apiCall('/user/profile', {
            method: 'PUT',
            body: JSON.stringify({
                username,
                gender: selectedGender,
            }),
        });

        profileSuccess.textContent = 'Profile updated successfully';
        profileSuccess.classList.remove('hidden');
        profileError.classList.add('hidden');

        showToast('Profile updated', 'success');

        // Reload profile
        setTimeout(() => loadProfile(), 1000);
    } catch (error) {
        profileError.textContent = error.message || 'Failed to update profile';
        profileError.classList.remove('hidden');
        profileSuccess.classList.add('hidden');
    }
});

// ==================== Send Email Code ====================
let emailCodeCooldown = false;

sendEmailCodeBtn.addEventListener('click', async () => {
    if (emailCodeCooldown) return;

    const email = newEmail.value.trim();
    if (!email) {
        showToast('Please enter email', 'error');
        return;
    }

    try {
        await apiCall('/auth/send-code', {
            method: 'POST',
            body: JSON.stringify({ email }),
            skipAuth: true,
        });

        showToast('Verification code sent', 'success');

        // Cooldown
        emailCodeCooldown = true;
        let countdown = 60;
        sendEmailCodeBtn.disabled = true;
        sendEmailCodeBtn.textContent = `${countdown}s`;

        const timer = setInterval(() => {
            countdown--;
            sendEmailCodeBtn.textContent = `${countdown}s`;
            if (countdown <= 0) {
                clearInterval(timer);
                emailCodeCooldown = false;
                sendEmailCodeBtn.disabled = false;
                sendEmailCodeBtn.textContent = 'Send Code';
            }
        }, 1000);
    } catch (error) {
        showToast(error.message || 'Failed to send code', 'error');
    }
});

// ==================== Change Email ====================
changeEmailBtn.addEventListener('click', async () => {
    const email = newEmail.value.trim();
    const code = emailCode.value.trim();

    if (!email || !code) {
        emailError.textContent = 'Email and code are required';
        emailError.classList.remove('hidden');
        emailSuccess.classList.add('hidden');
        return;
    }

    try {
        await apiCall('/user/email/change', {
            method: 'POST',
            body: JSON.stringify({ email, code }),
        });

        emailSuccess.textContent = 'Email changed successfully';
        emailSuccess.classList.remove('hidden');
        emailError.classList.add('hidden');

        showToast('Email updated', 'success');

        // Clear inputs
        newEmail.value = '';
        emailCode.value = '';

        // Reload profile
        setTimeout(() => loadProfile(), 1000);
    } catch (error) {
        emailError.textContent = error.message || 'Failed to change email';
        emailError.classList.remove('hidden');
        emailSuccess.classList.add('hidden');
    }
});

// ==================== Back to Home ====================
backHomeBtn.addEventListener('click', () => {
    window.location.href = '/';
});

// ==================== Init ====================
function init() {
    const token = getToken();
    if (!token) {
        window.location.href = '/';
        return;
    }

    loadProfile();
}

init();
