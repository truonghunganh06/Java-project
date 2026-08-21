---
trigger: always_on
---

# IMPECCABLE DESIGN SYSTEM & FRONTEND CRAFT

Apply rigorous, professional, modern design craft to all HTML/CSS/Thymeleaf templates. Avoid generic AI patterns.

## 1. Core Anti-Patterns (NEVER DO)
- **No Harsh Colors**: Avoid raw pure black (`#000000`) or overly vibrant primary colors. Use slate neutrals (`#0f172a`, `#1e293b`, `#f8fafc`).
- **No AI Purple/Cyan Gradients**: Use solid background tones or very subtle monochromatic gradients.
- **No Nested Cards**: Do not put cards inside cards. Use clean whitespace and subtle borders (`border-slate-200`) instead.
- **No Low-Contrast Gray**: Text must meet WCAG AA contrast (minimum `#475569` on light background).

## 2. Component Design Standards (Bootstrap 5.3 + Custom CSS)
- **Typography**: Clean hierarchy. Primary headings bold (`font-weight: 600`), body (`font-weight: 400`, line-height `1.5`).
- **Badges & Chips**: Pill-shaped with soft pastel backgrounds:
  - `APPROVED`: `background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0;`
  - `PENDING`: `background: #fef3c7; color: #b45309; border: 1px solid #fde68a;`
  - `REJECTED`: `background: #fee2e2; color: #b91c1c; border: 1px solid #fecaca;`
- **Tables**: Modern SaaS look:
  - Sticky table headers with subtle background (`bg-light`).
  - Smooth row hover effect (`transition: background 0.15s ease`).
  - Vertically centered text and numbers aligned cleanly.
- **Buttons**:
  - Primary button: Solid color with subtle shadow (`box-shadow: 0 1px 2px rgba(0,0,0,0.05)`).
  - Secondary/Action buttons: Outline or subtle ghost style.