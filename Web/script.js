// ============================================
// HUFFMAN COMPRESSION (matching Java implementation)
// ============================================

class HuffmanNode {
    constructor(ch, f, l = null, r = null) {
        this.ch = ch;
        this.f = f;
        this.l = l;
        this.r = r;
    }
}

function huffmanCompress(text) {
    if (!text || text.length === 0) return '\u0000\u0000\u0000\u0000';
    
    const freq = new Map();
    for (const c of text) {
        freq.set(c, (freq.get(c) || 0) + 1);
    }
    
    const pq = Array.from(freq.entries()).map(([ch, f]) => new HuffmanNode(ch, f));
    
    while (pq.length > 1) {
        pq.sort((a, b) => a.f - b.f);
        const a = pq.shift();
        const b = pq.shift();
        pq.push(new HuffmanNode('\0', a.f + b.f, a, b));
    }
    
    const root = pq[0];
    const codes = new Map();
    
    if (freq.size === 1) {
        codes.set(root.ch, '0');
    } else {
        generateCodes(root, '', codes);
    }
    
    const treeStr = serializeTree(root);
    const treeLen = treeStr.length;
    
    console.log('[Compress] Tree length:', treeLen, 'Tree sample:', treeStr.substring(0, 50));
    
    // Encode tree length as 4 characters (32 bits, supports up to 4 billion chars)
    const lenPrefix = String.fromCharCode(
        (treeLen >>> 24) & 0xFF,
        (treeLen >>> 16) & 0xFF,
        (treeLen >>> 8) & 0xFF,
        treeLen & 0xFF
    );
    
    let result = lenPrefix + treeStr;
    for (const c of text) {
        result += codes.get(c);
    }
    
    console.log('[Compress] Total result length:', result.length, '(4 byte header + tree + data)');
    return result;
}

function generateCodes(n, code, codes) {
    if (!n) return;
    if (n.l === null && n.r === null) {
        codes.set(n.ch, code === '' ? '0' : code);
        return;
    }
    generateCodes(n.l, code + '0', codes);
    generateCodes(n.r, code + '1', codes);
}

function serializeTree(n) {
    if (n.l === null && n.r === null) {
        return '1' + n.ch.charCodeAt(0).toString(2).padStart(8, '0');
    }
    return '0' + serializeTree(n.l) + serializeTree(n.r);
}

function huffmanDecompress(data) {
    if (data.length <= 4) return '';
    
    // Decode tree length from first 4 characters (32-bit unsigned integer)
    const treeLen = (
        (data.charCodeAt(0) << 24) |
        (data.charCodeAt(1) << 16) |
        (data.charCodeAt(2) << 8) |
        data.charCodeAt(3)
    ) >>> 0;
    
    if (treeLen <= 0 || treeLen > data.length - 4) {
        console.error('[Huffman] Invalid tree length:', treeLen, 'data length:', data.length);
        return '';
    }
    
    const treeStr = data.substring(4, 4 + treeLen);
    const compressedData = data.substring(4 + treeLen);
    
    console.log('[Huffman] Tree length:', treeLen, 'Data length:', compressedData.length);
    
    const pos = { val: 0 };
    const root = deserializeTree(treeStr, pos);
    if (!root) {
        console.error('[Huffman] Failed to deserialize tree');
        return '';
    }
    
    if (root.l === null && root.r === null) {
        let out = '';
        for (let i = 0; i < compressedData.length; i++) {
            out += root.ch;
        }
        return out;
    }
    
    let out = '';
    let cur = root;
    for (const bit of compressedData) {
        if (!cur) {
            console.error('[Huffman] Tree traversal failed - cur is null at bit:', bit);
            throw new Error('Huffman tree traversal failed');
        }
        cur = (bit === '0') ? cur.l : cur.r;
        if (!cur) {
            console.error('[Huffman] Invalid tree node - no child for bit:', bit);
            throw new Error('Invalid Huffman tree structure');
        }
        if (cur.l === null && cur.r === null) {
            out += cur.ch;
            cur = root;
        }
    }
    
    return out;
}

function deserializeTree(d, pos) {
    if (pos.val >= d.length) {
        console.error('[Huffman] Deserialize error: position', pos.val, 'exceeds data length', d.length);
        return null;
    }
    
    const t = d[pos.val++];
    if (t === '1') {
        const start = pos.val;
        pos.val += 8;
        if (pos.val > d.length) {
            console.error('[Huffman] Deserialize error: not enough data for character code');
            return null;
        }
        const charBits = d.substring(start, pos.val);
        const charCode = parseInt(charBits, 2);
        return new HuffmanNode(String.fromCharCode(charCode), 0);
    }
    
    const left = deserializeTree(d, pos);
    const right = deserializeTree(d, pos);
    if (!left || !right) {
        console.error('[Huffman] Deserialize error: failed to build child nodes');
        return null;
    }
    return new HuffmanNode('\0', 0, left, right);
}

// ============================================
// TEXT <-> BINARY (matching Java implementation)
// ============================================

// For ENCODE/DECODE: Simple 8-bit binary (no compression)
function textToBinary(text) {
    // Encode text as UTF-8 bytes first to handle emojis and special characters
    const encoder = new TextEncoder(); // UTF-8 encoder
    const utf8Bytes = encoder.encode(text);
    
    let b = '';
    for (const byte of utf8Bytes) {
        b += byte.toString(2).padStart(8, '0');
    }
    
    console.log('[TextToBinary] Text length:', text.length, 'UTF-8 bytes:', utf8Bytes.length, 'Binary length:', b.length, 'Divisible by 8:', b.length % 8 === 0);
    return b;
}

// For TEXT↔BINARY CONVERTER: With Huffman compression
function textToBinaryWithHuffman(text) {
    const h = huffmanCompress(text);
    // Huffman output is already ASCII (0s, 1s, and control chars), safe to use charCodeAt
    let b = '';
    for (const c of h) {
        b += c.charCodeAt(0).toString(2).padStart(8, '0');
    }
    return b;
}

// For ENCODE/DECODE: Simple 8-bit binary (no compression)
function binaryToText(binary) {
    console.log('[BinaryToText] Binary length:', binary.length, 'Divisible by 8:', binary.length % 8 === 0);
    if (binary.length % 8 !== 0) {
        console.error('[BinaryToText] ERROR: Binary length not divisible by 8! Remainder:', binary.length % 8);
        throw new Error('There is no hidden message');
    }
    
    // Decode UTF-8 bytes back to text
    const bytes = new Uint8Array(binary.length / 8);
    for (let i = 0; i < binary.length; i += 8) {
        const byteStr = binary.substring(i, i + 8);
        bytes[i / 8] = parseInt(byteStr, 2);
    }
    
    const decoder = new TextDecoder(); // UTF-8 decoder
    const text = decoder.decode(bytes);
    
    console.log('[BinaryToText] Decoded bytes:', bytes.length, 'Text length:', text.length);
    return text;
}

// For TEXT↔BINARY CONVERTER: With Huffman compression
function binaryToTextWithHuffman(binary) {
    if (binary.length % 8 !== 0) {
        throw new Error('Invalid binary format');
    }
    // Reconstruct Huffman string from binary
    let h = '';
    for (let i = 0; i < binary.length; i += 8) {
        const byteStr = binary.substring(i, i + 8);
        const val = parseInt(byteStr, 2);
        h += String.fromCharCode(val);
    }
    // Decompress and return (Huffman handles UTF-8 internally)
    const result = huffmanDecompress(h);
    return result;
}

// ============================================
// LSB STEGANOGRAPHY (matching Java implementation)
// ============================================

function calculateCapacity(width, height) {
    return width * height * 3;
}

function encodeToImage(imageData, binaryData, headerBits) {
    const data = new Uint8ClampedArray(imageData.data);
    const msgLen = binaryData.length;
    
    // Create header
    const header = msgLen.toString(2).padStart(headerBits, '0');
    const full = header + binaryData;
    
    console.log('[Encode] Message length:', msgLen);
    console.log('[Encode] Header:', header);
    console.log('[Encode] Full length:', full.length);
    
    let di = 0;
    
    // Encode pixel by pixel
    outerLoop: for (let i = 0; i < data.length; i += 4) {
        if (di >= full.length) break outerLoop;
        
        // R
        if (di < full.length) {
            data[i] = (data[i] & 0xFE) | parseInt(full[di]);
            di++;
        }
        // G
        if (di < full.length) {
            data[i + 1] = (data[i + 1] & 0xFE) | parseInt(full[di]);
            di++;
        }
        // B
        if (di < full.length) {
            data[i + 2] = (data[i + 2] & 0xFE) | parseInt(full[di]);
            di++;
        }
        // Ensure Alpha is fully opaque to prevent PNG issues
        data[i + 3] = 255;
    }
    
    console.log('[Encode] Bits written:', di);
    
    return new ImageData(data, imageData.width, imageData.height);
}

function decodeFromImage(imageData, headerBits) {
    const data = imageData.data;
    const maxPossible = imageData.width * imageData.height * 3;
    
    console.log('[Decode] Image dimensions:', imageData.width, 'x', imageData.height);
    console.log('[Decode] Max possible bits:', maxPossible);
    console.log('[Decode] Header bits:', headerBits);
    
    // Debug: show first 48 bits (the header) with their pixel indices
    const first48 = [];
    for (let i = 0, bitCount = 0; bitCount < 48 && i < data.length; i += 4) {
        first48.push((data[i] & 1).toString());
        bitCount++;
        if (bitCount >= 48) break;
        first48.push((data[i + 1] & 1).toString());
        bitCount++;
        if (bitCount >= 48) break;
        first48.push((data[i + 2] & 1).toString());
        bitCount++;
    }
    console.log('[Decode] First 48 LSBs (header):', first48.join(''));
    
    if (maxPossible < headerBits) {
        throw new Error('There is no hidden message');
    }
    
    // Read all bits sequentially from R,G,B channels
    // Use array for better performance with large data
    const bitsArray = [];
    let bitsRead = 0;
    let messageLength = 0;
    let totalNeeded = 0;
    
    outerLoop: for (let i = 0; i < data.length && bitsRead < headerBits + 10000000; i += 4) {
        // Read from R, G, B channels in order
        bitsArray.push((data[i] & 1).toString());
        bitsRead++;
        
        bitsArray.push((data[i + 1] & 1).toString());
        bitsRead++;
        
        bitsArray.push((data[i + 2] & 1).toString());
        bitsRead++;
        
        // Check if we have header to determine total needed bits
        if (bitsRead >= headerBits && messageLength === 0) {
            const headerBinary = bitsArray.slice(0, headerBits).join('');
            messageLength = parseInt(headerBinary, 2);
            
            if (messageLength <= 0 || messageLength > 100000000) {
                throw new Error('There is no hidden message');
            }
            
            totalNeeded = headerBits + messageLength;
            console.log('[Decode] Detected message length:', messageLength, 'Total needed:', totalNeeded);
        }
        
        // Stop reading once we have all data
        if (totalNeeded > 0 && bitsRead >= totalNeeded) {
            break outerLoop;
        }
    }
    
    console.log('[Decode] Bits read:', bitsRead, 'Converting to string...');
    const allBits = bitsArray.join('');
    console.log('[Decode] Conversion complete, total length:', allBits.length);
    
    // Extract header and validate
    if (allBits.length < headerBits) {
        console.error('[Decode] ERROR: Not enough bits read. Got:', allBits.length, 'Need:', headerBits);
        throw new Error('There is no hidden message');
    }
    
    const headerBinary = allBits.substring(0, headerBits);
    const messageBitLength = parseInt(headerBinary, 2);
    
    console.log('[Decode] Header binary:', headerBinary);
    console.log('[Decode] Message bit length:', messageBitLength);
    console.log('[Decode] Total bits read:', allBits.length);
    console.log('[Decode] Expected total (header + message):', headerBits + messageBitLength);
    
    if (messageBitLength <= 0) {
        console.error('[Decode] ERROR: Message length is zero or negative:', messageBitLength);
        throw new Error('There is no hidden message');
    }
    
    if (messageBitLength > allBits.length - headerBits) {
        console.error('[Decode] ERROR: Message length exceeds available bits. Message needs:', messageBitLength, 'Available:', allBits.length - headerBits);
        throw new Error('There is no hidden message');
    }
    
    // Extract message binary (skip header)
    const messageBinary = allBits.substring(headerBits, headerBits + messageBitLength);
    
    console.log('[Decode] Extracted message binary length:', messageBinary.length);
    
    // Validate binary format
    if (messageBinary.length % 8 !== 0) {
        console.error('[Decode] ERROR: Message binary not divisible by 8. Length:', messageBinary.length, 'Remainder:', messageBinary.length % 8);
        throw new Error('There is no hidden message');
    }
    
    console.log('[Decode] Message binary length:', messageBinary.length);
    console.log('[Decode] Decoded successfully');
    
    return messageBinary;
}

// ============================================
// IMAGE MANIPULATION WITHOUT CANVAS
// ============================================

function imageToImageData(img) {
    const canvas = document.createElement('canvas');
    canvas.width = img.width;
    canvas.height = img.height;
    const ctx = canvas.getContext('2d', {
        willReadFrequently: true,
        alpha: false,
        colorSpace: 'srgb'
    });
    ctx.imageSmoothingEnabled = false;
    ctx.drawImage(img, 0, 0);
    return ctx.getImageData(0, 0, canvas.width, canvas.height);
}

function imageDataToDataURL(imageData) {
    const canvas = document.createElement('canvas');
    canvas.width = imageData.width;
    canvas.height = imageData.height;
    const ctx = canvas.getContext('2d', {
        alpha: true,
        colorSpace: 'srgb',
        willReadFrequently: false,
        desynchronized: false
    });
    ctx.imageSmoothingEnabled = false;
    ctx.putImageData(imageData, 0, 0);
    // Use maximum quality PNG to preserve exact pixel values
    return canvas.toDataURL('image/png', 1.0);
}

// ============================================
// UI CONTROLLER
// ============================================

class SteganographyApp {
    constructor() {
        this.encodedImageUrl = null;
        this.huffmanTreeData = null;
        this.imageCapacity = null;
        this.textSize = null;
        
        this.initEventListeners();
    }

    initEventListeners() {
        // Tab switching
        document.querySelectorAll('.tab-trigger').forEach(trigger => {
            trigger.addEventListener('click', () => this.switchTab(trigger.dataset.tab));
        });

        // Encode tab
        document.getElementById('encode-image').addEventListener('change', (e) => this.handleImageUpload(e, 'encode-preview'));
        document.getElementById('encode-btn').addEventListener('click', () => this.encodeMessage());
        document.getElementById('download-btn').addEventListener('click', () => this.downloadImage());

        // Decode tab
        document.getElementById('decode-image').addEventListener('change', (e) => this.handleImageUpload(e, 'decode-preview'));
        document.getElementById('decode-btn').addEventListener('click', () => this.decodeMessage());

        // Converter tab
        document.getElementById('text-to-binary-btn').addEventListener('click', () => this.textToBinaryConvert());
        document.getElementById('binary-to-text-btn').addEventListener('click', () => this.binaryToTextConvert());

        // Capacity tab
        document.getElementById('capacity-image').addEventListener('change', (e) => this.handleImageUpload(e, 'capacity-preview'));
        document.getElementById('capacity-btn').addEventListener('click', () => this.checkCapacity());
        document.getElementById('calculate-text-size-btn').addEventListener('click', () => this.calculateTextSize());
        document.getElementById('capacity-text-input').addEventListener('input', () => this.updateTextSizeOnType());
    }

    switchTab(tabName) {
        document.querySelectorAll('.tab-trigger').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
        
        document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
        document.getElementById(`${tabName}-content`).classList.add('active');
    }

    handleImageUpload(event, previewId) {
        const file = event.target.files[0];
        if (!file) return;

        if (!file.type.startsWith('image/')) {
            this.showAlert('Please select a valid image file', 'error', previewId.replace('-preview', '-alert'));
            return;
        }

        const reader = new FileReader();
        reader.onload = (e) => {
            const preview = document.getElementById(previewId);
            preview.innerHTML = `<img src="${e.target.result}" alt="Preview">`;
        };
        reader.readAsDataURL(file);
    }

    async encodeMessage() {
        const imageInput = document.getElementById('encode-image');
        const messageInput = document.getElementById('secret-message');
        const headerBits = parseInt(document.getElementById('header-bits').value);

        if (!imageInput.files[0]) {
            this.showAlert('Please select an image first', 'error', 'encode-alert');
            return;
        }

        if (!messageInput.value.trim()) {
            this.showAlert('Please enter a secret message', 'error', 'encode-alert');
            return;
        }

        try {
            const img = await this.loadImage(imageInput.files[0]);
            const capacity = calculateCapacity(img.width, img.height);
            
            console.log('[Main] Converting text to binary...');
            const binaryData = textToBinary(messageInput.value);
            
            console.log('[Main] Binary data length:', binaryData.length);
            console.log('[Main] Capacity:', capacity);
            
            // Validate that headerBits is large enough to encode the message length
            const maxHeaderRepresentable = Math.pow(2, headerBits);
            if (binaryData.length >= maxHeaderRepresentable) {
                this.showAlert(`Message is too large! Maximum supported message size is ${Math.floor(maxHeaderRepresentable / 8).toLocaleString()} bytes.`, 'error', 'encode-alert');
                return;
            }

            const totalBits = headerBits + binaryData.length;

            if (totalBits > capacity) {
                this.showAlert(`Message too large! Image can hold ${capacity} bits, but message needs ${totalBits} bits.`, 'error', 'encode-alert');
                return;
            }

            if (totalBits > capacity * 0.8) {
                this.showAlert('Warning: Message uses more than 80% of image capacity.', 'warning', 'encode-alert');
            }

            const imageData = imageToImageData(img);
            const encodedImageData = encodeToImage(imageData, binaryData, headerBits);
            this.encodedImageUrl = imageDataToDataURL(encodedImageData);

            const preview = document.getElementById('encoded-preview');
            preview.innerHTML = `<img src="${this.encodedImageUrl}" alt="Encoded">`;

            document.getElementById('download-btn').disabled = false;
            
            document.getElementById('encode-capacity').textContent = `${capacity.toLocaleString()} bits`;
            document.getElementById('encode-message-size').textContent = `${totalBits.toLocaleString()} bits`;
            document.getElementById('encode-stats').style.display = 'block';

            this.showAlert('Message encoded successfully!', 'success', 'encode-alert');
            
            // VERIFICATION TEST: Try to decode immediately to verify encoding worked
            console.log('[Verify] Testing immediate decode...');
            try {
                const testImageData = imageToImageData(await this.loadImageFromURL(this.encodedImageUrl));
                const testBinary = decodeFromImage(testImageData, headerBits);
                const testMessage = binaryToText(testBinary);
                console.log('[Verify] ✅ Immediate decode successful:', testMessage);
                console.log('[Verify] Match:', testMessage === messageInput.value);
            } catch (verifyErr) {
                console.error('[Verify] ❌ Immediate decode FAILED:', verifyErr.message);
            }
        } catch (err) {
            console.error('[Main] Encode error:', err);
            this.showAlert('Failed to encode message: ' + err.message, 'error', 'encode-alert');
        }
    }
    
    loadImageFromURL(url) {
        return new Promise((resolve, reject) => {
            const img = new Image();
            img.onload = () => resolve(img);
            img.onerror = reject;
            img.src = url;
        });
    }

    async decodeMessage() {
        const imageInput = document.getElementById('decode-image');
        const headerBits = parseInt(document.getElementById('decode-header-bits').value);

        if (!imageInput.files[0]) {
            this.showAlert('Please select an image first', 'error', 'decode-alert');
            return;
        }

        try {
            const img = await this.loadImage(imageInput.files[0]);

            console.log('[Main] Decoding from image...');
            const imageData = imageToImageData(img);
            const binaryData = decodeFromImage(imageData, headerBits);

            console.log('[Main] Converting binary to text...');
            const message = binaryToText(binaryData);

            console.log('[Main] Decoded message:', message);

            const output = document.getElementById('decoded-output');
            output.innerHTML = `
                <div class="decoded-message">
                    <div class="decoded-label">Secret Message</div>
                    <div style="margin-top: 0.5rem; word-break: break-word;">${this.escapeHtml(message)}</div>
                </div>
            `;

            this.showAlert('Message decoded successfully!', 'success', 'decode-alert');
        } catch (err) {
            console.error('[Main] Decode error:', err);
            this.showAlert('There is no hidden message', 'error', 'decode-alert');
        }
    }

    textToBinaryConvert() {
        const textInput = document.getElementById('text-input');

        if (!textInput.value.trim()) {
            this.showAlert('Please enter some text to convert', 'error', 'converter-alert');
            return;
        }

        try {
            // Use Huffman compression in converter tab
            const binary = textToBinaryWithHuffman(textInput.value);
            
            document.getElementById('binary-output').value = binary;
            document.getElementById('binary-output-group').style.display = 'flex';
            document.getElementById('binary-input').value = binary;

            this.showAlert('Text converted to binary successfully!', 'success', 'converter-alert');
        } catch (err) {
            console.error(err);
            this.showAlert('Failed to convert text to binary: ' + err.message, 'error', 'converter-alert');
        }
    }

    binaryToTextConvert() {
        const binaryInput = document.getElementById('binary-input');

        if (!binaryInput.value.trim()) {
            this.showAlert('Please enter binary data to convert', 'error', 'converter-alert');
            return;
        }

        if (!/^[01]+$/.test(binaryInput.value)) {
            this.showAlert('Binary input must contain only 0s and 1s', 'error', 'converter-alert');
            return;
        }

        try {
            // Use Huffman decompression in converter tab
            const result = binaryToTextWithHuffman(binaryInput.value);
            
            document.getElementById('text-output').value = result;
            document.getElementById('text-output-group').style.display = 'flex';

            this.showAlert('Binary converted to text successfully!', 'success', 'converter-alert');
        } catch (err) {
            console.error(err);
            this.showAlert('There is no hidden message', 'error', 'converter-alert');
        }
    }

    async checkCapacity() {
        const imageInput = document.getElementById('capacity-image');

        if (!imageInput.files[0]) {
            this.showAlert('Please select an image first', 'error', 'capacity-alert');
            return;
        }

        try {
            const img = await this.loadImage(imageInput.files[0]);

            const totalBits = calculateCapacity(img.width, img.height);
            const totalBytes = Math.floor(totalBits / 8);
            const totalKB = (totalBytes / 1024).toFixed(2);
            // Realistic character estimate: subtract 48-bit header, assume average 8 bits per character
            // (Emojis and special chars will use more space)
            const usableBits = totalBits - 48; // Subtract header
            const estimatedChars = Math.floor(usableBits / 8); // 8 bits per character

            console.log('[Capacity] Image:', img.width, 'x', img.height);
            console.log('[Capacity] Total bits:', totalBits);
            console.log('[Capacity] Estimated chars:', estimatedChars);

            document.getElementById('cap-dimensions').textContent = `${img.width} × ${img.height}`;
            document.getElementById('cap-pixels').textContent = (img.width * img.height).toLocaleString();
            document.getElementById('cap-bits').textContent = totalBits.toLocaleString();
            document.getElementById('cap-bytes').textContent = totalBytes.toLocaleString();
            document.getElementById('cap-kb').textContent = totalKB;
            document.getElementById('cap-chars').textContent = estimatedChars.toLocaleString();

            document.getElementById('capacity-results').style.display = 'block';

            // Store capacity for comparison
            this.imageCapacity = totalBits;

            // Update comparison if text is already calculated
            this.updateCapacityComparison();

            this.showAlert(`This image can hide approximately ${estimatedChars.toLocaleString()} characters (emojis and special characters may use more space).`, 'info', 'capacity-alert');
        } catch (err) {
            console.error('[Capacity] Error:', err);
            this.showAlert('Failed to analyze image capacity: ' + err.message, 'error', 'capacity-alert');
        }
    }

    calculateTextSize() {
        const textInput = document.getElementById('capacity-text-input');

        if (!textInput.value.trim()) {
            this.showAlert('Please enter some text to analyze', 'error', 'capacity-alert');
            return;
        }

        try {
            const text = textInput.value;
            
            // Calculate actual size using the REAL encoding method (binary, no compression)
            const actualBinaryData = textToBinary(text);
            const messageSize = actualBinaryData.length;
            const totalSize = 48 + messageSize; // 48-bit header + data
            
            const charCount = text.length;

            // Show ACTUAL sizes that will be used in encoding
            document.getElementById('text-char-count').textContent = charCount.toLocaleString();
            document.getElementById('text-original-size').textContent = `${messageSize.toLocaleString()} bits (${(messageSize / 8).toLocaleString()} bytes)`;
            document.getElementById('text-total-size').textContent = `${totalSize.toLocaleString()} bits (${(totalSize / 8).toFixed(1)} bytes)`;

            document.getElementById('text-size-results').style.display = 'block';

            // Store ACTUAL text size for accurate capacity comparison
            this.textSize = totalSize;

            // Update comparison with accurate size
            this.updateCapacityComparison();

            this.showAlert('Text size calculated successfully!', 'success', 'capacity-alert');
        } catch (err) {
            console.error('[Text Size] Error:', err);
            this.showAlert('Failed to calculate text size: ' + err.message, 'error', 'capacity-alert');
        }
    }

    updateTextSizeOnType() {
        const textInput = document.getElementById('capacity-text-input');
        if (textInput.value.trim() && document.getElementById('text-size-results').style.display !== 'none') {
            this.calculateTextSize();
        }
    }

    updateCapacityComparison() {
        if (!this.imageCapacity || !this.textSize) {
            return;
        }

        const comparisonDiv = document.getElementById('capacity-comparison');
        const resultBox = comparisonDiv.querySelector('.capacity-result-box');
        const icon = document.getElementById('capacity-result-icon');
        const text = document.getElementById('capacity-result-text');

        comparisonDiv.style.display = 'block';

        if (this.textSize <= this.imageCapacity) {
            // Text fits!
            resultBox.className = 'capacity-result-box success';
            icon.textContent = '✓';
            const percentUsed = ((this.textSize / this.imageCapacity) * 100).toFixed(1);
            text.textContent = `✅ Your text will fit! It uses ${percentUsed}% of the image capacity (${this.textSize.toLocaleString()} / ${this.imageCapacity.toLocaleString()} bits)`;
        } else {
            // Text doesn't fit
            resultBox.className = 'capacity-result-box error';
            icon.textContent = '✗';
            const exceededBy = this.textSize - this.imageCapacity;
            text.textContent = `❌ Your text is too large! It exceeds capacity by ${exceededBy.toLocaleString()} bits. Try a larger image or shorter text.`;
        }
    }

    loadImage(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = (e) => {
                const img = new Image();
                img.onload = () => resolve(img);
                img.onerror = reject;
                img.src = e.target.result;
            };
            reader.onerror = reject;
            reader.readAsDataURL(file);
        });
    }

    downloadImage() {
        if (!this.encodedImageUrl) return;

        console.log('[Download] Image URL length:', this.encodedImageUrl.length);
        
        const link = document.createElement('a');
        link.href = this.encodedImageUrl;
        link.download = 'encoded-image.png';
        link.click();
        
        console.log('[Download] Image download triggered');
    }

    showAlert(message, type, alertId) {
        const alert = document.getElementById(alertId);
        alert.textContent = message;
        alert.className = `alert alert-${type}`;
        alert.style.display = 'block';

        setTimeout(() => {
            alert.style.display = 'none';
        }, 5000);
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Initialize the app
document.addEventListener('DOMContentLoaded', () => {
    new SteganographyApp();
});
