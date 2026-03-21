// Moment System JavaScript
// This file handles Moment creation, viewing, and management

const API_BASE = '/api';
const TOKEN_KEY = 'bf_token';

let currentMomentPage = 0;
let currentMyMomentPage = 0;
let selectedVisibility = 'PUBLIC';
let momentImages = [];
let templates = [];

// ==================== Utility Functions ====================
function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

function authHeader() {
    const token = getToken();
    return token ? { 'Authorization': `Bearer ${token}` } : {};
}

function showToast(message, type = 'normal') {
    const toast = document.getElementById('toast');
    if (toast) {
        toast.textContent = message;
        toast.className = `toast ${type} show`;
        setTimeout(() => toast.classList.remove('show'), 3000);
    }
}

// ==================== Load Templates ====================
async function loadTemplates() {
    try {
        const response = await fetch(`${API_BASE}/moment/templates`, {
            headers: authHeader()
        });
        if (!response.ok) throw new Error('Failed to load templates');
        templates = await response.json();

        const select = document.getElementById('moment-template');
        templates.forEach(t => {
            const option = document.createElement('option');
            option.value = t.id;
            option.textContent = `${t.name} - ${t.description}`;
            select.appendChild(option);
        });
    } catch (error) {
        console.error('Failed to load templates:', error);
    }
}

// ==================== Image Upload ====================
const momentImageDropzone = document.getElementById('moment-image-dropzone');
const momentImageInput = document.getElementById('moment-image-input');
const momentImagePreview = document.getElementById('moment-image-preview');
const momentImagePlaceholder = document.getElementById('moment-image-placeholder');

momentImageDropzone.addEventListener('click', () => momentImageInput.click());
momentImageDropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    momentImageDropzone.classList.add('dragover');
});
momentImageDropzone.addEventListener('dragleave', () => {
    momentImageDropzone.classList.remove('dragover');
});
momentImageDropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    momentImageDropzone.classList.remove('dragover');
    const files = Array.from(e.dataTransfer.files).filter(f => f.type.startsWith('image/'));
    handleMomentImages(files);
});

momentImageInput.addEventListener('change', (e) => {
    handleMomentImages(Array.from(e.target.files));
});

function handleMomentImages(files) {
    if (momentImages.length + files.length > 9) {
        showToast('Maximum 9 images allowed', 'error');
        return;
    }

    files.forEach(file => {
        if (file.size > 10 * 1024 * 1024) {
            showToast(`${file.name} exceeds 10MB limit`, 'error');
            return;
        }
        momentImages.push(file);
    });

    renderMomentImagePreview();
}

function renderMomentImagePreview() {
    if (momentImages.length === 0) {
        momentImagePreview.classList.add('hidden');
        momentImagePlaceholder.classList.remove('hidden');
        return;
    }

    momentImagePreview.classList.remove('hidden');
    momentImagePlaceholder.classList.add('hidden');
    momentImagePreview.innerHTML = '';

    momentImages.forEach((file, index) => {
        const reader = new FileReader();
        reader.onload = (e) => {
            const div = document.createElement('div');
            div.className = 'preview-item';
            div.innerHTML = `
                <img src="${e.target.result}" alt="preview">
                <button class="preview-remove" data-index="${index}">&times;</button>
            `;
            momentImagePreview.appendChild(div);

            div.querySelector('.preview-remove').addEventListener('click', (ev) => {
                ev.stopPropagation();
                momentImages.splice(index, 1);
                renderMomentImagePreview();
            });
        };
        reader.readAsDataURL(file);
    });
}

// ==================== Visibility Selection ====================
const visibilityNav = document.getElementById('moment-visibility-nav');
const userSelectGroup = document.getElementById('moment-user-select-group');

visibilityNav.addEventListener('click', (e) => {
    const btn = e.target.closest('.pill');
    if (!btn) return;

    visibilityNav.querySelectorAll('.pill').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    selectedVisibility = btn.dataset.visibility;

    if (selectedVisibility === 'VISIBLE_TO' || selectedVisibility === 'HIDDEN_FROM') {
        userSelectGroup.classList.remove('hidden');
    } else {
        userSelectGroup.classList.add('hidden');
    }
});

// ==================== Create Moment ====================
document.getElementById('create-moment-btn').addEventListener('click', async () => {
    const textContent = document.getElementById('moment-text').value.trim();
    const templateId = document.getElementById('moment-template').value;
    const userIds = document.getElementById('moment-user-ids').value.trim();

    if (!textContent && momentImages.length === 0) {
        showToast('Please add content or images', 'error');
        return;
    }

    const formData = new FormData();
    if (textContent) formData.append('textContent', textContent);
    if (templateId) formData.append('templateId', templateId);
    formData.append('visibility', selectedVisibility);
    if (userIds) formData.append('visibleUserIds', userIds);

    momentImages.forEach(file => {
        formData.append('images', file);
    });

    try {
        const response = await fetch(`${API_BASE}/moment`, {
            method: 'POST',
            headers: authHeader(),
            body: formData
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to create moment');
        }

        showToast('Moment created successfully', 'success');
        document.getElementById('moment-text').value = '';
        document.getElementById('moment-template').value = '';
        document.getElementById('moment-user-ids').value = '';
        momentImages = [];
        renderMomentImagePreview();

        // Switch to My Moments tab
        document.querySelector('[data-tab="moment-my"]').click();
    } catch (error) {
        showToast(error.message, 'error');
    }
});

// ==================== Load My Moments ====================
async function loadMyMoments(page = 0) {
    try {
        const response = await fetch(`${API_BASE}/moment/my?page=${page}&size=10`, {
            headers: authHeader()
        });
        if (!response.ok) throw new Error('Failed to load moments');

        const data = await response.json();
        const list = document.getElementById('my-moments-list');

        if (page === 0) list.innerHTML = '';

        if (data.content.length === 0 && page === 0) {
            list.innerHTML = '<div class="card"><p class="muted">No moments yet. Create your first moment!</p></div>';
            return;
        }

        console.log('Loaded moments:', data.content); // Debug log

        data.content.forEach(moment => {
            console.log('Moment images:', moment.images); // Debug log
            list.appendChild(createMomentCard(moment, true));
        });

        currentMyMomentPage = page;
        document.getElementById('load-more-my').style.display = data.last ? 'none' : 'block';
    } catch (error) {
        showToast('Failed to load moments', 'error');
    }
}

document.getElementById('load-more-my').addEventListener('click', () => {
    loadMyMoments(currentMyMomentPage + 1);
});

// ==================== Generate Share Link ====================
document.getElementById('generate-share-link-btn').addEventListener('click', async () => {
    try {
        const response = await fetch(`${API_BASE}/moment/share/generate`, {
            method: 'POST',
            headers: authHeader()
        });
        if (!response.ok) throw new Error('Failed to generate share link');

        const data = await response.json();
        const shareUrl = `${window.location.origin}/moment-share.html?code=${data.shareCode}`;

        document.getElementById('share-link-url').value = shareUrl;
        document.getElementById('share-link-card').classList.remove('hidden');
        showToast('Share link generated', 'success');
    } catch (error) {
        showToast(error.message, 'error');
    }
});

document.getElementById('copy-share-link-btn').addEventListener('click', () => {
    const input = document.getElementById('share-link-url');
    input.select();
    document.execCommand('copy');
    showToast('Link copied to clipboard', 'success');
});

// ==================== Create Moment Card ====================
function createMomentCard(moment, showActions = false) {
    const card = document.createElement('div');
    card.className = 'card mt';

    const date = new Date(moment.createdAt).toLocaleString();
    const visibilityBadge = `<span class="badge badge-${moment.visibility.toLowerCase()}">${moment.visibility}</span>`;

    console.log('Creating card for moment:', moment.id, 'Images:', moment.images); // Debug

    let imagesHtml = '';
    if (moment.images && moment.images.length > 0) {
        console.log('Rendering', moment.images.length, 'images'); // Debug
        imagesHtml = '<div class="moment-images">';
        moment.images.forEach(img => {
            console.log('Image URL:', img.imageUrl); // Debug
            if (img.livePhoto) {
                imagesHtml += `
                    <div class="moment-image-item">
                        <img src="${img.imageUrl}" alt="moment image">
                        <span class="live-badge">LIVE</span>
                    </div>
                `;
            } else {
                imagesHtml += `<img src="${img.imageUrl}" alt="moment image" class="moment-image-item">`;
            }
        });
        imagesHtml += '</div>';
    } else {
        console.log('No images found for moment', moment.id); // Debug
    }

    let actionsHtml = '';
    if (showActions) {
        actionsHtml = `
            <div class="moment-actions">
                <button class="btn btn-secondary btn-sm" onclick="deleteMoment(${moment.id})">Delete</button>
            </div>
        `;
    }

    card.innerHTML = `
        <div class="moment-header">
            <div>
                <strong>User ID: ${moment.userId}</strong>
                <small class="muted"> • ${date}</small>
            </div>
            ${visibilityBadge}
        </div>
        <div class="moment-content">
            ${moment.textContent ? `<p>${escapeHtml(moment.textContent)}</p>` : ''}
            ${moment.htmlContent ? `<div class="moment-html">${moment.htmlContent}</div>` : ''}
            ${imagesHtml}
        </div>
        ${actionsHtml}
    `;

    return card;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ==================== Delete Moment ====================
async function deleteMoment(id) {
    if (!confirm('Are you sure you want to delete this moment?')) return;

    try {
        const response = await fetch(`${API_BASE}/moment/${id}`, {
            method: 'DELETE',
            headers: authHeader()
        });

        if (!response.ok) throw new Error('Failed to delete moment');

        showToast('Moment deleted', 'success');
        loadMyMoments(0);
    } catch (error) {
        showToast(error.message, 'error');
    }
}

// ==================== Initialize ====================
function initMoment() {
    if (!getToken()) return;

    loadTemplates();
    loadTimeline(0);
    loadMyMoments(0);
}

// Make deleteMoment available globally
window.deleteMoment = deleteMoment;

// ==================== Timeline ====================
async function loadTimeline(page = 0) {
    try {
        const response = await fetch(`${API_BASE}/moment/timeline?page=${page}&size=10`, {
            headers: authHeader()
        });
        if (!response.ok) throw new Error('Failed to load timeline');

        const data = await response.json();
        const list = document.getElementById('timeline-list');

        if (page === 0) list.innerHTML = '';

        if (data.content.length === 0 && page === 0) {
            list.innerHTML = '<div class="card"><p class="muted">No moments available</p></div>';
            return;
        }

        data.content.forEach(moment => {
            list.appendChild(createMomentCard(moment, false));
        });

        currentMomentPage = page;
        document.getElementById('load-more-timeline').style.display = data.last ? 'none' : 'block';
    } catch (error) {
        showToast('Failed to load timeline', 'error');
    }
}

document.getElementById('load-more-timeline').addEventListener('click', () => {
    loadTimeline(currentMomentPage + 1);
});





