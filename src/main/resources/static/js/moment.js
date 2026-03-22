// Moment System JavaScript
// This file handles Moment creation, viewing, and management

const API_BASE = '/api';
const TOKEN_KEY = 'bf_token';

let currentMomentPage = 0;
let currentMyMomentPage = 0;
let selectedVisibility = 'PUBLIC';
let momentImages = [];

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

// 防抖函数
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// 节流函数
function throttle(func, limit) {
    let inThrottle;
    return function(...args) {
        if (!inThrottle) {
            func.apply(this, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

// 图片懒加载
function lazyLoadImage(img) {
    if ('loading' in HTMLImageElement.prototype) {
        img.loading = 'lazy';
    } else {
        // Fallback for browsers that don't support lazy loading
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const image = entry.target;
                    image.src = image.dataset.src;
                    image.classList.add('loaded');
                    observer.unobserve(image);
                }
            });
        });
        observer.observe(img);
    }
}

// ==================== Image Upload ====================
let momentImageDropzone, momentImageInput, momentImagePreview, momentImagePlaceholder;

function initImageUpload() {
    momentImageDropzone = document.getElementById('moment-image-dropzone');
    momentImageInput = document.getElementById('moment-image-input');
    momentImagePreview = document.getElementById('moment-image-preview');
    momentImagePlaceholder = document.getElementById('moment-image-placeholder');

    if (!momentImageDropzone || !momentImageInput) {
        console.error('Image upload elements not found');
        return;
    }

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
}

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
    const cardMode = document.getElementById('moment-card-mode').checked;
    const userIds = document.getElementById('moment-user-ids').value.trim();

    if (!textContent && momentImages.length === 0) {
        showToast('Please add content or images', 'error');
        return;
    }

    const formData = new FormData();
    if (textContent) formData.append('textContent', textContent);
    formData.append('cardMode', cardMode);
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
        document.getElementById('moment-card-mode').checked = false;
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

        console.log('Loaded moments:', data.content);

        data.content.forEach(moment => {
            console.log('Moment images:', moment.images);
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
        console.log('Rendering', moment.images.length, 'images, cardMode:', moment.cardMode); // Debug

        if (moment.cardMode) {
            // 使用卡片堆叠模式
            imagesHtml = createCardStack(moment.images, moment.id);
        } else {
            // 使用普通网格模式
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
        }
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
            <div style="display: flex; align-items: center; gap: 10px;">
                ${moment.avatar ? `<img src="${moment.avatar}" alt="avatar" style="width: 32px; height: 32px; border-radius: 50%; object-fit: cover;">` : ''}
                <div>
                    <strong>${moment.username || 'User ' + moment.userId}</strong>
                    <small class="muted"> • ${date}</small>
                </div>
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

    // 如果是卡片堆叠模式，初始化交互
    if (moment.cardMode && moment.images && moment.images.length > 0) {
        setTimeout(() => initCardStack(card, moment.id), 0);
    }

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






// ==================== Card Stack Functions ====================
function createCardStack(images, momentId) {
    const stackId = `stack-${momentId}`;
    let html = `
        <div class="moment-card-stack" id="${stackId}">
            <div class="moment-card-counter">
                <span class="current">1</span> / ${images.length}
            </div>
    `;

    images.forEach((img, index) => {
        const classes = index === 0 ? 'active' : index === 1 ? 'next' : index === 2 ? 'after' : 'hidden';
        html += `
            <div class="moment-card-stack-item ${classes}" data-index="${index}">
                <img src="${img.imageUrl}" alt="moment image">
            </div>
        `;
    });

    html += `
            <div class="moment-card-nav">
                ${images.map((_, i) => `<div class="moment-card-dot ${i === 0 ? 'active' : ''}" data-index="${i}"></div>`).join('')}
            </div>
        </div>
    `;

    return html;
}

function initCardStack(card, momentId) {
    const stackId = `stack-${momentId}`;
    const stack = card.querySelector(`#${stackId}`);
    if (!stack) return;

    const items = stack.querySelectorAll('.moment-card-stack-item');
    const dots = stack.querySelectorAll('.moment-card-dot');
    const counter = stack.querySelector('.moment-card-counter .current');
    let currentIndex = 0;
    let startX = 0;
    let isDragging = false;

    function updateStack(newIndex) {
        if (newIndex < 0 || newIndex >= items.length) return;

        currentIndex = newIndex;

        items.forEach((item, i) => {
            item.classList.remove('active', 'next', 'after', 'hidden');
            if (i === currentIndex) {
                item.classList.add('active');
            } else if (i === currentIndex + 1) {
                item.classList.add('next');
            } else if (i === currentIndex + 2) {
                item.classList.add('after');
            } else {
                item.classList.add('hidden');
            }
        });

        dots.forEach((dot, i) => {
            dot.classList.toggle('active', i === currentIndex);
        });

        counter.textContent = currentIndex + 1;
    }

    // Touch/Mouse events for swiping
    stack.addEventListener('mousedown', (e) => {
        startX = e.clientX;
        isDragging = true;
    });

    stack.addEventListener('touchstart', (e) => {
        startX = e.touches[0].clientX;
        isDragging = true;
    });

    stack.addEventListener('mousemove', (e) => {
        if (!isDragging) return;
        const diff = e.clientX - startX;
        if (Math.abs(diff) > 50) {
            if (diff > 0 && currentIndex > 0) {
                updateStack(currentIndex - 1);
            } else if (diff < 0 && currentIndex < items.length - 1) {
                updateStack(currentIndex + 1);
            }
            isDragging = false;
        }
    });

    stack.addEventListener('touchmove', (e) => {
        if (!isDragging) return;
        const diff = e.touches[0].clientX - startX;
        if (Math.abs(diff) > 50) {
            if (diff > 0 && currentIndex > 0) {
                updateStack(currentIndex - 1);
            } else if (diff < 0 && currentIndex < items.length - 1) {
                updateStack(currentIndex + 1);
            }
            isDragging = false;
        }
    });

    stack.addEventListener('mouseup', () => isDragging = false);
    stack.addEventListener('touchend', () => isDragging = false);

    // Wheel event for scrolling through cards with debounce
    const handleWheel = debounce((e) => {
        e.preventDefault();

        // deltaY > 0 表示向下滚动，显示下一张
        // deltaY < 0 表示向上滚动，显示上一张
        if (e.deltaY > 0 && currentIndex < items.length - 1) {
            updateStack(currentIndex + 1);
        } else if (e.deltaY < 0 && currentIndex > 0) {
            updateStack(currentIndex - 1);
        }
    }, 100);

    stack.addEventListener('wheel', handleWheel, { passive: false });

    // Dot navigation
    dots.forEach((dot, index) => {
        dot.addEventListener('click', () => updateStack(index));
    });
}

// ==================== Tab Refresh ====================
function initTabRefresh() {
    const momentNav = document.getElementById('moment-nav');
    if (!momentNav) return;

    momentNav.addEventListener('click', (e) => {
        const btn = e.target.closest('.pill');
        if (!btn) return;

        const tab = btn.dataset.tab;

        // 当切换到Timeline或My Moments时，刷新数据
        if (tab === 'moment-timeline') {
            loadTimeline(0);
            // 标记为已读
            markTimelineAsRead();
        } else if (tab === 'moment-my') {
            loadMyMoments(0);
        }
    });
}

// ==================== Unread Count ====================
async function updateUnreadCount() {
    try {
        const response = await fetch(`${API_BASE}/moment/unread-count`, {
            headers: authHeader()
        });
        if (!response.ok) return;

        const count = await response.json();
        const badge = document.getElementById('moment-unread-badge');
        if (badge) {
            if (count > 0) {
                badge.textContent = count > 99 ? '99+' : count;
                badge.classList.remove('hidden');
            } else {
                badge.classList.add('hidden');
            }
        }
    } catch (error) {
        console.error('Failed to update unread count:', error);
    }
}

async function markTimelineAsRead() {
    try {
        await fetch(`${API_BASE}/moment/mark-read`, {
            method: 'POST',
            headers: authHeader()
        });
        // 清除红点
        const badge = document.getElementById('moment-unread-badge');
        if (badge) {
            badge.classList.add('hidden');
        }
    } catch (error) {
        console.error('Failed to mark as read:', error);
    }
}

// 定期检查未读数量（每30秒）
function startUnreadCountPolling() {
    updateUnreadCount(); // 立即执行一次
    setInterval(updateUnreadCount, 30000); // 每30秒检查一次
}

// ==================== Initialize ====================
// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        initImageUpload();
        initTabRefresh();
        // 如果用户已登录，启动未读数量轮询
        if (getToken()) {
            startUnreadCountPolling();
        }
    });
} else {
    initImageUpload();
    initTabRefresh();
    if (getToken()) {
        startUnreadCountPolling();
    }
}
