---
name: Terra & Sage
colors:
  surface: '#fff8f5'
  surface-dim: '#e1d8d4'
  surface-bright: '#fff8f5'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#fbf2ed'
  surface-container: '#f5ece7'
  surface-container-high: '#efe6e2'
  surface-container-highest: '#e9e1dc'
  on-surface: '#1e1b18'
  on-surface-variant: '#55433d'
  inverse-surface: '#34302c'
  inverse-on-surface: '#f8efea'
  outline: '#88726c'
  outline-variant: '#dbc1b9'
  surface-tint: '#99462a'
  primary: '#99462a'
  on-primary: '#ffffff'
  primary-container: '#d97757'
  on-primary-container: '#541400'
  inverse-primary: '#ffb59e'
  secondary: '#486459'
  on-secondary: '#ffffff'
  secondary-container: '#c8e7d9'
  on-secondary-container: '#4c685d'
  tertiary: '#7c5800'
  on-tertiary: '#ffffff'
  tertiary-container: '#bd890c'
  on-tertiary-container: '#3b2800'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbd0'
  primary-fixed-dim: '#ffb59e'
  on-primary-fixed: '#390b00'
  on-primary-fixed-variant: '#7a2f15'
  secondary-fixed: '#cae9db'
  secondary-fixed-dim: '#afcdc0'
  on-secondary-fixed: '#042018'
  on-secondary-fixed-variant: '#314c42'
  tertiary-fixed: '#ffdea7'
  tertiary-fixed-dim: '#f8bd45'
  on-tertiary-fixed: '#271900'
  on-tertiary-fixed-variant: '#5e4200'
  background: '#fff8f5'
  on-background: '#1e1b18'
  surface-variant: '#e9e1dc'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 56px
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '800'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 8px
  sm: 16px
  md: 24px
  lg: 40px
  xl: 64px
  gutter: 20px
  margin-mobile: 20px
  margin-desktop: auto
  max-width-content: 1200px
---

## Brand & Style
The design system is centered on a warm, organic, and encouraging atmosphere tailored for language learners. It avoids the clinical or corporate "tech blue" in favor of an earth-toned palette that feels grounded yet energetic. 

The style is **Modern Minimalist with Tactile accents**. It leverages heavy whitespace and high-quality typography to ensure clarity, while using soft, organic color washes to prevent the UI from feeling cold. The emotional response should be one of "gentle momentum"—making the daunting task of learning a new language feel like a sunny, approachable journey.

## Colors
This design system utilizes a palette inspired by natural pigments to create a welcoming environment.

- **Primary (Terracotta):** Used for main actions, progress indicators, and brand-heavy elements. It provides heat and energy without the aggression of pure red.
- **Secondary (Sage Green):** Reserved for "Success" states, secondary buttons, and achievement markers. It provides a calming contrast to the terracotta.
- **Tertiary (Warm Amber):** Used for highlighting tips, streaks, and motivational alerts.
- **Neutral (Charcoal):** A warm-toned off-black used for text to maintain high contrast while avoiding the harshness of #000.
- **Background (Cream):** A soft off-white background reduces eye strain during long study sessions compared to pure white.

## Typography
The typography strategy pairs **Plus Jakarta Sans** for headings to provide a friendly, slightly rounded geometric character, with **Inter** for body text to ensure maximum legibility and functional clarity.

- Use **Display** styles for milestone celebrations and welcome screens.
- Use **Headline** styles for lesson titles and navigation headers.
- **Body-lg** is the default for lesson content and translated phrases to ensure high readability for learners.
- **Labels** are always semi-bold or bold to ensure buttons and tabs are easily identifiable at a glance.

## Layout & Spacing
The layout follows a **Fluid Grid** system with fixed maximum widths for desktop to maintain readability of text-heavy lessons.

- **Mobile:** 4-column grid with 20px margins.
- **Tablet/Desktop:** 12-column grid with 20px gutters. Content is centered with a maximum width of 1200px.
- **Vertical Rhythm:** Use a strict 8px-based spacing scale. Internal component padding (like button labels) can use 4px increments, but layout-level spacing must use 16px, 24px, or 40px gaps to maintain a clean, airy feel.

## Elevation & Depth
This design system avoids heavy shadows in favor of **Tonal Layers** and **Soft Ambient Occlusion**.

- **Level 0 (Background):** Cream (#FFFBF7).
- **Level 1 (Cards/Surface):** Pure White (#FFFFFF). Used for interactive elements like lesson cards or input areas.
- **Level 2 (Active/Floating):** White with a very soft, diffused shadow (Blur: 20px, Y: 4px, Color: 4% Charcoal).
- **Interactive Depth:** When a button is pressed, it should "sink" (remove shadow and shift Y by 1px) to provide a tactile, physical response.

## Shapes
The shape language is consistently **Rounded**. Sharp corners are avoided to maintain the "welcoming" brand promise.

- **Standard Elements:** 0.5rem (8px) for buttons, inputs, and small cards.
- **Large Containers:** 1rem (16px) for main lesson modules or modal overlays.
- **Selection States:** Use a thick 2px border in the Primary color rather than just a color fill to indicate focus, aiding accessibility.

## Components

### Buttons
- **Primary:** Terracotta background with White text. Bold weight. High-tack tactile feel.
- **Secondary:** Sage Green background with White text. Used for "Check Answer" or "Continue."
- **Ghost:** Charcoal text with no background. Used for "Skip" or "Back."

### Chips/Tags
- Used for word selection in sentence-building exercises. 
- Background: Very light tint of Terracotta (5% opacity). 
- Border: 1px solid Terracotta (20% opacity).
- Text: Charcoal.

### Cards
- Lesson cards use a white background with a 1px border (#E5E0DA).
- On hover/active, the border thickens to 2px Primary (Terracotta) and gains a Level 2 shadow.

### Input Fields
- Underlined or fully boxed with 8px radius.
- Focus state: 2px Primary border with a soft glow in the same color.

### Progress Bars
- Background: Sage Green (15% opacity).
- Fill: Sage Green (Solid). 
- Height: 12px with fully rounded ends.

### Interactive "Flashcards"
- Large 1rem radius. 
- Center-aligned display typography. 
- Use subtle haptic feedback patterns (visual bounce) when flipped.