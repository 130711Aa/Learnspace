---
name: Executive Scholar OS
colors:
  surface: '#f7f9fb'
  surface-dim: '#d8dadc'
  surface-bright: '#f7f9fb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f6'
  surface-container: '#eceef0'
  surface-container-high: '#e6e8ea'
  surface-container-highest: '#e0e3e5'
  on-surface: '#191c1e'
  on-surface-variant: '#45474c'
  inverse-surface: '#2d3133'
  inverse-on-surface: '#eff1f3'
  outline: '#75777d'
  outline-variant: '#c5c6cd'
  surface-tint: '#545f73'
  primary: '#091426'
  on-primary: '#ffffff'
  primary-container: '#1e293b'
  on-primary-container: '#8590a6'
  inverse-primary: '#bcc7de'
  secondary: '#3f5d9c'
  on-secondary: '#ffffff'
  secondary-container: '#9bb8fe'
  on-secondary-container: '#284785'
  tertiary: '#001815'
  on-tertiary: '#ffffff'
  tertiary-container: '#002f2a'
  on-tertiary-container: '#28a094'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e3fb'
  primary-fixed-dim: '#bcc7de'
  on-primary-fixed: '#111c2d'
  on-primary-fixed-variant: '#3c475a'
  secondary-fixed: '#d9e2ff'
  secondary-fixed-dim: '#afc6ff'
  on-secondary-fixed: '#001944'
  on-secondary-fixed-variant: '#254583'
  tertiary-fixed: '#89f5e7'
  tertiary-fixed-dim: '#6bd8cb'
  on-tertiary-fixed: '#00201d'
  on-tertiary-fixed-variant: '#005049'
  background: '#f7f9fb'
  on-background: '#191c1e'
  surface-variant: '#e0e3e5'
typography:
  display-lg:
    fontFamily: Hanken Grotesk
    fontSize: 40px
    fontWeight: '600'
    lineHeight: 48px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Hanken Grotesk
    fontSize: 30px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.015em
  headline-xl:
    fontFamily: Hanken Grotesk
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Hanken Grotesk
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Hanken Grotesk
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.005em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
    letterSpacing: 0em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: 0em
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: 0.01em
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: -0.005em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-caps:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.06em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-tablet: 1.5rem
  margin: 1rem
  margin-tablet: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1.25rem
  space-xl: 2rem
---

## Brand & Style

This design system delivers a focused, distraction-free spatial computing environment engineered for serious academic and intellectual pursuit on high-end tablet hardware. It rejects the hyper-gamified, neon, cartoonish patterns common in ed-tech in favor of an authoritative, calm executive aesthetic that treats the student as an autonomous researcher, thinker, and writer.

### Aesthetic Direction
- **Modern Minimalist with Architectural Structure:** Clean, grid-aligned, high-whitespace layouts framed by delicate 1px structural dividers.
- **Material Utility & Spatial Ergonomics:** Tuned specifically for landscape and portrait tablet manipulation, dual-pane splitting, stylus precision, and direct finger manipulation.
- **Discreet Elegance:** Zero unnecessary decorative textures, zero AI iridescent multi-stop gradients. Visual excitement originates solely from the user's content, disciplined typography, and deep, curated subject taxonomy accents.

## Colors

The palette grounds the interface in quiet, mineral-inspired neutrals with targeted accents reserved exclusively for categorical taxonomy, focal actions, and active study sessions.

### Structural Neutrals
- **Canvas Base (`#F8FAFC` light / `#0B0F17` dark):** Soft alabaster canvas preventing eye fatigue during multi-hour reading sessions.
- **Surface Elevation (`#FFFFFF` light / `#131B26` dark):** Crisp white floating sheets and reading canvases.
- **Borders & Dividers (`#E2E8F0` light / `#1E293B` dark):** Precise 1px architectural lines defining split views, floating tools, and inspector rails.
- **Primary Ink (`#0F172A` light / `#F1F5F9` dark):** High-contrast graphite for deep reading and headline retention.
- **Muted Ink (`#64748B` light / `#94A3B8` dark):** De-emphasized metadata, timestamps, and secondary references.

### Subject Discipline Accents (Semantic Taxonomy)
Color is applied semantically to distinguish subjects without noise:
- **Formal Sciences & Logic (Deep Indigo):** `#3730A3` light / `#818CF8` dark
- **Natural & Biological Sciences (Sage Emerald):** `#065F46` light / `#34D399` dark
- **Historical & Human Sciences (Warm Amber):** `#92400E` light / `#FBBF24` dark
- **Literature, Languages & Arts (Dusty Violet):** `#581C87` light / `#C084FC` dark

### Application Rules
- Interactive primary touch states inherit the slate primary (`#1E293B`).
- Subject tags use 10% tint fills with solid 100% ink text. Never apply heavy gradients across structural headers or backgrounds.

## Typography

The typographic hierarchy balances structural precision with effortless long-form reading.

- **Headlines (Hanken Grotesk):** Provides a sharp, contemporary, architectural quality without feeling clinical. Used for workspace headers, document titles, modal headers, and subject shelf titles.
- **Body & Controls (Inter):** Highly legible screen workhorse engineered for dense outlines, dual-pane PDF annotations, flashcard summaries, and low-latency stylus text conversion.
- **Letter Spacing:** Headlines utilize slight negative tracking (-0.01em to -0.02em) to maintain visual density. Micro-labels and categorical subject badges leverage positive tracking (0.02em to 0.06em) for rapid scanning.

## Layout & Spacing

The spatial architecture centers around tablet productivity, featuring a compact vertical navigation rail, primary split panes, and floating contextual panels.

### Grid & Rails
- **Navigation Rail:** 72px fixed-width left rail on tablet landscapes housing core destinations (Library, Canvas, Syllabus, Review, Settings). Expands to a 240px drawer only on explicit gesture.
- **12-Column Fluid Grid:** Tablet workspaces follow a 12-column dynamic grid with `1.5rem` gutters.
- **Split Views:** Standard 60/40 reading-to-notes split or symmetrical 50/50 dual-document comparison. Each active panel maintains internal outer margins of `1.25rem`.

### Touch & Stylus Target Boundaries
- All interactive targets enforce a strict minimum footprint of 48px × 48px to accommodate both natural finger taps and passive stylus precision.
- Inline text controls retain a minimum 44px vertical target bounding box, even when glyph rendering is compact.

## Elevation & Depth

Visual separation relies on surface tonality and hairline perimeter strokes rather than theatrical drop shadows.

### Elevation Hierarchy
1. **Canvas Surface (Level 0):** `#F8FAFC` base layer. No shadow, flat backdrop.
2. **Structural Panes & Cards (Level 1):** `#FFFFFF` surface bounded by a continuous 1px `#E2E8F0` hairline border. Shadow: `0 1px 2px 0 rgba(15, 23, 42, 0.04)`.
3. **Floating Palettes & Toolbars (Level 2):** Floating pen docks, floating searchbars, and context popovers. Shadow: `0 4px 12px -2px rgba(15, 23, 42, 0.08), 0 2px 6px -1px rgba(15, 23, 42, 0.04)`. Bounded by 1px `#CBD5E1`.
4. **Modal Overlays & Inspectors (Level 3):** Modal study decks and contextual inspectors. Shadow: `0 12px 24px -4px rgba(15, 23, 42, 0.12)`. Supported by a 20% slate backdrop dim.

### Dark Mode Adaptation
In dark mode, elevations transition from deep slate `#0B0F17` (Level 0) to `#131B26` (Level 1) and `#1E293B` (Level 2/3), relying predominantly on `#334155` border contrast rather than shadow spread.

## Shapes

The design system employs moderate corner radii (Level 2 - `rounded-md` 0.5rem base, `rounded-lg` 1rem, `rounded-xl` 1.5rem) to evoke an ergonomic, handheld physical device feel without becoming playful or toy-like.

- **Primary Buttons, Inputs & List Items:** 0.5rem (8px). Ensures crisp, compact interaction edges that feel intentional and sturdy.
- **Subject Modules, Document Tiles, Surface Panes:** 1rem (16px). Softens the expansive surface area of tablet windows.
- **Floating Tool Docks & Contextual Sheets:** 1.5rem (24px). Indicates touchable, movable objects distinct from the underlying page.
- **Badges & Taxonomy Pills:** Full pill (9999px) strictly for micro-status indicators and categorical tags.

## Components

### Buttons & Interactive Triggers
- **Primary Action:** Solid `#1E293B` background, `#FFFFFF` text, 0.5rem radius, 48px height, medium weight (`label-lg`). Active state introduces a subtle scale transform (`0.98`) without heavy ripples.
- **Secondary / Outline:** Transparent background, 1px `#E2E8F0` border, `#0F172A` text. Hover/touch state shifts to `#F1F5F9`.
- **Icon / Stylus Triggers:** Minimum 48px × 48px square container with centered 20px vector icons. Borderless resting state, light tinted background on focus.

### Chips & Subject Tags
- Height: 28px. Padding: 4px horizontal, 10px vertical.
- Border radius: Full pill.
- Appearance: Semantic tint background (10% opacity) with a solid 1px matching border at 25% opacity and matching high-contrast foreground text. Never use saturated backgrounds with inverted white text for metadata tags.

### Input Fields & Search Bars
- 48px height, 1px `#CBD5E1` border resting, transitioning to 1.5px `#1E293B` on focus.
- Background: `#FFFFFF` flat.
- Typography: `body-md` with `#94A3B8` placeholder ink. Left-aligned subject-filtered dropdown pills optionally integrated inside the left inner pad.

### Cards & Document Nodes
- Background: `#FFFFFF`.
- Perimeter: 1px continuous hairline `#E2E8F0`.
- Padding: 16px internal padding.
- Organization: Left indicator rail (3px accent line) displaying the subject color taxonomy, followed by an outline title, reading timestamp, and completion metric.

### Selection Controls (Checkboxes & Radios)
- Checkboxes: 20px × 20px box, 4px corner radius, 1.5px `#94A3B8` border resting. Checked: `#1E293B` fill with crisp white checkmark vector.
- Minimum 48px circular hit zone centered around the control.

### Tablet-Specific Native OS Elements
- **Stylus Floating Palette:** Slim horizontal pill floating 24px above the bottom bezel. Houses pen, highlighter, lasso, eraser, and active subject color picker with an explicit 48px touch boundary per tool.
- **Split-View Divider Handle:** 4px wide rounded bar centered in an 8px gutter with a subtle vertical grip affordance, offering tactile drag resistance between active workspace viewports.