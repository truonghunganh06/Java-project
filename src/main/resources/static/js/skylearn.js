/**
 * SKYLEARN DESIGN SYSTEM - INTERACTIVE JAVASCRIPT SUITE
 * Supports Confetti celebrations, Answer tile feedback, Reading ruler,
 * Mascot hints, Speech synthesis (Read to me), and View mode switcher.
 */

window.Skylearn = (function () {
    'use strict';

    // 1. Confetti Burst Celebration (1.6-second duration with Sky, Sun, Leaf, Berry colors)
    function triggerConfetti(originX, originY) {
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            console.log('Reduced motion active: skipping confetti animation.');
            return;
        }

        const colors = ['#3B82F6', '#60A5FA', '#FBBF24', '#FCD34D', '#22C55E', '#A855F7'];
        const confettiCount = 60;
        const container = document.createElement('div');
        container.style.position = 'fixed';
        container.style.inset = '0';
        container.style.pointerEvents = 'none';
        container.style.zIndex = '9999';
        container.style.overflow = 'hidden';
        document.body.appendChild(container);

        const startX = originX || window.innerWidth / 2;
        const startY = originY || 100;

        for (let i = 0; i < confettiCount; i++) {
            const piece = document.createElement('div');
            const color = colors[Math.floor(Math.random() * colors.length)];
            const size = Math.floor(Math.random() * 8) + 8;
            const angle = Math.random() * Math.PI * 2;
            const velocity = Math.random() * 300 + 150;
            const destX = startX + Math.cos(angle) * velocity;
            const destY = startY + Math.sin(angle) * velocity + 200;
            const rot = Math.random() * 720 - 360;

            piece.style.position = 'absolute';
            piece.style.left = `${startX}px`;
            piece.style.top = `${startY}px`;
            piece.style.width = `${size}px`;
            piece.style.height = `${size * 1.2}px`;
            piece.style.backgroundColor = color;
            piece.style.borderRadius = '4px';
            piece.style.transition = 'all 1.6s cubic-bezier(0.25, 1, 0.5, 1)';
            piece.style.opacity = '1';

            container.appendChild(piece);

            requestAnimationFrame(() => {
                piece.style.transform = `translate(${destX - startX}px, ${destY - startY}px) rotate(${rot}deg)`;
                piece.style.opacity = '0';
            });
        }

        setTimeout(() => {
            if (container.parentNode) {
                container.parentNode.removeChild(container);
            }
        }, 1800);
    }

    // 2. Reading Passage - Ruler Toggle & Tracker
    function initReadingRuler() {
        const passages = document.querySelectorAll('.skylearn-reading-passage');
        passages.forEach(passage => {
            let ruler = passage.querySelector('.skylearn-reading-ruler');
            if (!ruler) {
                ruler = document.createElement('div');
                ruler.className = 'skylearn-reading-ruler';
                passage.appendChild(ruler);
            }

            passage.addEventListener('mousemove', (e) => {
                const rect = passage.getBoundingClientRect();
                const relY = e.clientY - rect.top;
                // Snap to roughly 32px line leading
                const snappedY = Math.max(10, Math.floor(relY / 32) * 32 + 8);
                ruler.style.top = `${snappedY}px`;
            });
        });
    }

    function toggleReadingRuler(targetPassageId) {
        const passage = targetPassageId ? document.getElementById(targetPassageId) : document.querySelector('.skylearn-reading-passage');
        if (passage) {
            passage.classList.toggle('reading-ruler-active');
            const isActive = passage.classList.contains('reading-ruler-active');
            return isActive;
        }
        return false;
    }

    // 3. Speech Synthesis: "Read to me"
    function readAloud(text, btnElement) {
        if (!('speechSynthesis' in window)) {
            alert('Trình duyệt của bạn không hỗ trợ tính năng đọc âm thanh (Speech Synthesis).');
            return;
        }

        window.speechSynthesis.cancel();

        if (!text) {
            const activePassage = document.querySelector('.skylearn-reading-passage');
            text = activePassage ? activePassage.innerText : '';
        }

        const utterance = new SpeechSynthesisUtterance(text);
        utterance.rate = 0.95; // Slightly slower for clarity
        utterance.lang = 'vi-VN';

        if (btnElement) {
            const originalHtml = btnElement.innerHTML;
            btnElement.innerHTML = '<i class="fa-solid fa-volume-high fa-beat text-sky"></i> Đang đọc...';
            utterance.onend = () => {
                btnElement.innerHTML = originalHtml;
            };
            utterance.onerror = () => {
                btnElement.innerHTML = originalHtml;
            };
        }

        window.speechSynthesis.speak(utterance);
    }

    // 4. Multiple Choice Answer Tile Handler
    function handleAnswerClick(tileElement, isCorrect, explanationText) {
        const parent = tileElement.closest('.skylearn-answers-group') || tileElement.parentElement;
        if (!parent) return;

        // Reset previous tiles in group
        parent.querySelectorAll('.skylearn-answer-tile').forEach(tile => {
            tile.classList.remove('is-correct', 'is-incorrect');
            const msg = tile.querySelector('.skylearn-answer-msg');
            if (msg) msg.remove();
        });

        if (isCorrect) {
            tileElement.classList.add('is-correct');
            // Add subtle leaf feedback message
            const feedback = document.createElement('div');
            feedback.className = 'skylearn-answer-msg text-leaf-deep font-bold mt-2 d-flex align-items-center gap-2';
            feedback.innerHTML = '<i class="fa-solid fa-circle-check text-leaf"></i> <span>Chính xác! Làm rất tốt.</span>';
            tileElement.appendChild(feedback);
        } else {
            tileElement.classList.add('is-incorrect');
            const feedback = document.createElement('div');
            feedback.className = 'skylearn-answer-msg text-coral-deep font-medium mt-2 d-flex align-items-center gap-2';
            feedback.innerHTML = `<i class="fa-solid fa-circle-info text-coral"></i> <span>Chưa đúng rồi — hãy thử lại nhé!</span> ${explanationText ? `<a href="javascript:void(0)" onclick="alert('${explanationText}')" class="text-sky ms-2 text-decoration-underline">Xem gợi ý</a>` : ''}`;
            tileElement.appendChild(feedback);
        }
    }

    // 5. Parent / Kid Mode Switcher
    function toggleViewMode(mode) {
        const body = document.body;
        if (mode === 'parent') {
            body.classList.add('mode-parent');
            body.classList.remove('mode-kid');
            localStorage.setItem('skylearn_view_mode', 'parent');
        } else {
            body.classList.add('mode-kid');
            body.classList.remove('mode-parent');
            localStorage.setItem('skylearn_view_mode', 'kid');
        }
    }

    // Initialize on DOM ready
    document.addEventListener('DOMContentLoaded', () => {
        initReadingRuler();
        const savedMode = localStorage.getItem('skylearn_view_mode') || 'kid';
        toggleViewMode(savedMode);
    });

    return {
        confetti: triggerConfetti,
        initReadingRuler: initReadingRuler,
        toggleReadingRuler: toggleReadingRuler,
        readAloud: readAloud,
        handleAnswer: handleAnswerClick,
        toggleViewMode: toggleViewMode
    };
})();
