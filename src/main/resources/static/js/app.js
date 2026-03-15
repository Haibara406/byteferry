// State
let currentType = 'text';
let selectedFiles = [];
let currentShareCode = null;

// --- Tab & Type Switching ---

function switchTab(tab) {
    document.getElementById('view-send').classList.toggle('hidden', tab !== 'send');
    document.getElementById('view-receive').classList.toggle('hidden', tab !== 'receive');
    document.getElementById('tab-send').classList.toggle('active', tab === 'send');
    document.getElementById('tab-receive').classList.toggle('active', tab !== 'send');
}

function switchType(type) {
    currentType = type;
    selectedFiles = [];
    document.querySelectorAll('.type-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.type === type);
    });
    document.querySelectorAll('.input-area').forEach(area => area.classList.add('hidden'));
    document.getElementById('input-' + type).classList.remove('hidden');
    resetUploadUI();
}

function resetUploadUI() {
    document.getElementById('image-preview-container').innerHTML = '';
    document.getElementById('image-preview-container').classList.add('hidden');
    document.getElementById('image-placeholder').classList.remove('hidden');
    document.getElementById('file-list').innerHTML = '';
    document.getElementById('file-list').classList.add('hidden');
    document.getElementById('file-placeholder').classList.remove('hidden');
    document.getElementById('send-result').classList.add('hidden');
}

// --- Drag & Drop ---

function setupDropZone(zoneId, inputId, handler) {
    const zone = document.getElementById(zoneId);
    if (!zone) return;

    zone.addEventListener('click', () => document.getElementById(inputId).click());

    zone.addEventListener('dragover', e => {
        e.preventDefault();
        zone.classList.add('drag-over');
    });
    zone.addEventListener('dragleave', () => zone.classList.remove('drag-over'));
    zone.addEventListener('drop', e => {
        e.preventDefault();
        zone.classList.remove('drag-over');
        if (e.dataTransfer.files.length > 0) {
            handler(Array.from(e.dataTransfer.files));
        }
    });
}

function handleImageSelect(event) {
    if (event.target.files.length > 0) {
        handleImages(Array.from(event.target.files));
    }
}

function handleFileSelect(event) {
    if (event.target.files.length > 0) {
        handleFilesSelected(Array.from(event.target.files));
    }
}

function handleImages(files) {
    selectedFiles = selectedFiles.concat(files);
    const container = document.getElementById('image-preview-container');
    container.classList.remove('hidden');
    document.getElementById('image-placeholder').classList.add('hidden');

    files.forEach(file => {
        const wrapper = document.createElement('div');
        wrapper.className = 'inline-block relative m-1';

        const img = document.createElement('img');
        img.className = 'h-24 rounded-lg shadow object-cover';
        img.alt = file.name;

        const removeBtn = document.createElement('button');
        removeBtn.className = 'absolute -top-1 -right-1 bg-red-500 text-white rounded-full w-5 h-5 text-xs flex items-center justify-center hover:bg-red-600';
        removeBtn.textContent = 'x';
        removeBtn.onclick = (e) => {
            e.stopPropagation();
            selectedFiles = selectedFiles.filter(f => f !== file);
            wrapper.remove();
            if (selectedFiles.length === 0) {
                container.classList.add('hidden');
                document.getElementById('image-placeholder').classList.remove('hidden');
            }
        };

        const reader = new FileReader();
        reader.onload = e => { img.src = e.target.result; };
        reader.readAsDataURL(file);

        wrapper.appendChild(img);
        wrapper.appendChild(removeBtn);
        container.appendChild(wrapper);
    });
}

function handleFilesSelected(files) {
    selectedFiles = selectedFiles.concat(files);
    renderFileList();
}

function renderFileList() {
    const list = document.getElementById('file-list');
    list.innerHTML = '';
    list.classList.remove('hidden');
    document.getElementById('file-placeholder').classList.add('hidden');

    selectedFiles.forEach((file, idx) => {
        const item = document.createElement('div');
        item.className = 'flex items-center justify-between py-2 px-3 bg-gray-50 rounded-lg mb-1.5';
        item.innerHTML = `
            <div class="flex-1 min-w-0 mr-3">
                <p class="text-sm text-gray-800 truncate">${file.name}</p>
                <p class="text-xs text-gray-400">${formatSize(file.size)}</p>
            </div>
            <button class="text-gray-400 hover:text-red-500 text-lg flex-shrink-0" onclick="removeFile(${idx})">x</button>
        `;
        list.appendChild(item);
    });

    if (selectedFiles.length === 0) {
        list.classList.add('hidden');
        document.getElementById('file-placeholder').classList.remove('hidden');
    }
}

function removeFile(idx) {
    selectedFiles.splice(idx, 1);
    renderFileList();
}

// --- Send ---

async function send() {
    const btn = document.getElementById('send-btn');
    btn.disabled = true;
    btn.textContent = 'Sending...';

    try {
        let response;
        const deleteAfter = document.getElementById('delete-after').checked;

        if (currentType === 'text') {
            const content = document.getElementById('text-content').value.trim();
            if (!content) {
                showToast('Please enter some text');
                return;
            }
            response = await fetch('/api/share/text', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content, deleteAfterDownload: deleteAfter })
            });
        } else {
            if (selectedFiles.length === 0) {
                showToast('Please select at least one file');
                return;
            }
            const formData = new FormData();
            selectedFiles.forEach(f => formData.append('file', f));
            formData.append('type', currentType.toUpperCase());
            formData.append('deleteAfterDownload', deleteAfter);
            response = await fetch('/api/share/file', {
                method: 'POST',
                body: formData
            });
        }

        if (!response.ok) {
            const err = await response.json().catch(() => ({}));
            throw new Error(err.message || err.error || 'Send failed');
        }

        const data = await response.json();
        currentShareCode = data.code;
        document.getElementById('share-code').textContent = data.code;
        document.getElementById('send-result').classList.remove('hidden');
        showToast('Sent successfully!');
    } catch (e) {
        showToast(e.message);
    } finally {
        btn.disabled = false;
        btn.textContent = 'Send';
    }
}

// --- Receive ---

async function receive() {
    const code = document.getElementById('receive-code').value.trim().toUpperCase();
    if (!code) {
        showToast('Please enter a share code');
        return;
    }

    // Reset
    document.getElementById('receive-result').classList.add('hidden');
    document.getElementById('receive-error').classList.add('hidden');
    document.getElementById('result-text').classList.add('hidden');
    document.getElementById('result-images').classList.add('hidden');
    document.getElementById('result-files').classList.add('hidden');
    document.getElementById('result-delete-warning').classList.add('hidden');

    try {
        const response = await fetch('/api/share/' + code);
        if (!response.ok) {
            const err = await response.json().catch(() => ({}));
            throw new Error(err.message || err.error || 'Share not found');
        }

        const data = await response.json();
        currentShareCode = code;

        document.getElementById('receive-result').classList.remove('hidden');

        if (data.deleteAfterDownload) {
            document.getElementById('result-delete-warning').classList.remove('hidden');
        }

        switch (data.type) {
            case 'TEXT':
                document.getElementById('result-text').classList.remove('hidden');
                document.getElementById('text-result-content').textContent = data.content;
                break;
            case 'IMAGE':
                renderImageResults(data.files, code);
                break;
            case 'FILE':
                renderFileResults(data.files, code);
                break;
        }
    } catch (e) {
        document.getElementById('receive-error').classList.remove('hidden');
        document.getElementById('error-message').textContent = e.message;
    }
}

function renderImageResults(files, code) {
    const container = document.getElementById('result-images');
    const grid = document.getElementById('image-results-grid');
    grid.innerHTML = '';
    container.classList.remove('hidden');

    files.forEach(f => {
        const card = document.createElement('div');
        card.className = 'bg-gray-50 rounded-lg overflow-hidden';
        card.innerHTML = `
            <img src="/api/share/${code}/preview/${f.index}" class="w-full object-contain max-h-64" alt="${f.fileName}">
            <div class="p-2 flex items-center justify-between">
                <span class="text-xs text-gray-500 truncate">${f.fileName}</span>
                <button class="text-xs text-blue-600 hover:text-blue-800 font-medium flex-shrink-0 ml-2" onclick="downloadSingle('${code}', ${f.index}, '${f.fileName}')">
                    Download
                </button>
            </div>
        `;
        grid.appendChild(card);
    });
}

function renderFileResults(files, code) {
    const container = document.getElementById('result-files');
    const list = document.getElementById('file-results-list');
    list.innerHTML = '';
    container.classList.remove('hidden');

    files.forEach(f => {
        const item = document.createElement('div');
        item.className = 'flex items-center space-x-3 py-3 px-4 bg-gray-50 rounded-lg mb-2';
        item.innerHTML = `
            <svg class="h-8 w-8 text-blue-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
            <div class="flex-1 min-w-0">
                <p class="text-sm text-gray-800 font-medium truncate">${f.fileName}</p>
                <p class="text-xs text-gray-400">${formatSize(f.fileSize)}</p>
            </div>
            <button class="px-3 py-1.5 bg-blue-600 text-white rounded-lg text-xs font-medium hover:bg-blue-700 transition-colors flex-shrink-0" onclick="downloadSingle('${code}', ${f.index}, '${f.fileName}')">
                Download
            </button>
        `;
        list.appendChild(item);
    });
}

// --- Actions ---

function copyCode() {
    const code = document.getElementById('share-code').textContent;
    navigator.clipboard.writeText(code).then(() => showToast('Code copied!'));
}

function copyContent() {
    const content = document.getElementById('text-result-content').textContent;
    navigator.clipboard.writeText(content).then(() => showToast('Content copied!'));
}

function downloadSingle(code, index, fileName) {
    const a = document.createElement('a');
    a.href = '/api/share/' + code + '/download/' + index;
    a.download = fileName || '';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
}

function downloadAll() {
    if (!currentShareCode) return;
    const items = document.querySelectorAll('#file-results-list button, #image-results-grid button');
    items.forEach(btn => btn.click());
}

// --- Utils ---

function formatSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return (bytes / Math.pow(k, i)).toFixed(1) + ' ' + sizes[i];
}

function showToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 2000);
}

// --- Init ---

document.addEventListener('DOMContentLoaded', () => {
    setupDropZone('image-drop-zone', 'image-input', handleImages);
    setupDropZone('file-drop-zone', 'file-input', handleFilesSelected);
});
