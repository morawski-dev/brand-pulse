/**
 * Animation Utilities - Reusable animation configs and helpers
 *
 * Contains common animation classes and transition configurations
 * for consistent UI animations across the application
 */

// ========================================
// SHIMMER ANIMATION
// ========================================

/**
 * Shimmer effect for loading skeletons
 * Usage: Add to className along with base skeleton styling
 */
export const shimmerAnimation = `
  relative overflow-hidden
  before:absolute before:inset-0
  before:-translate-x-full
  before:animate-[shimmer_2s_infinite]
  before:bg-gradient-to-r
  before:from-transparent before:via-white/60 before:to-transparent
`;

// ========================================
// FADE ANIMATIONS
// ========================================

export const fadeIn = 'animate-in fade-in duration-300';
export const fadeOut = 'animate-out fade-out duration-200';

// ========================================
// SLIDE ANIMATIONS
// ========================================

export const slideInFromTop = 'animate-in slide-in-from-top-2 duration-300';
export const slideInFromBottom = 'animate-in slide-in-from-bottom-2 duration-300';
export const slideInFromLeft = 'animate-in slide-in-from-left-2 duration-300';
export const slideInFromRight = 'animate-in slide-in-from-right-2 duration-300';

// ========================================
// SCALE ANIMATIONS
// ========================================

export const scaleIn = 'animate-in zoom-in-95 duration-200';
export const scaleOut = 'animate-out zoom-out-95 duration-150';

// ========================================
// TRANSITION CONFIGS
// ========================================

export const transitions = {
  fast: 'transition-all duration-150 ease-in-out',
  normal: 'transition-all duration-300 ease-in-out',
  slow: 'transition-all duration-500 ease-in-out',

  // Specific properties
  colors: 'transition-colors duration-200 ease-in-out',
  transform: 'transition-transform duration-300 ease-in-out',
  opacity: 'transition-opacity duration-200 ease-in-out',
  shadow: 'transition-shadow duration-200 ease-in-out',
} as const;

// ========================================
// HOVER EFFECTS
// ========================================

export const hoverEffects = {
  lift: 'hover:-translate-y-0.5 hover:shadow-lg transition-all duration-200',
  scale: 'hover:scale-105 transition-transform duration-200',
  glow: 'hover:shadow-lg hover:shadow-blue-500/20 transition-shadow duration-200',
  brighten: 'hover:brightness-110 transition-all duration-200',
} as const;

// ========================================
// LOADING STATES
// ========================================

export const loadingStates = {
  pulse: 'animate-pulse',
  spin: 'animate-spin',
  bounce: 'animate-bounce',
} as const;

// ========================================
// SKELETON STYLES
// ========================================

export const skeleton = {
  base: 'bg-gray-200 rounded animate-pulse',
  shimmer: `bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 bg-[length:200%_100%] animate-shimmer`,
  text: 'h-4 bg-gray-200 rounded',
  circle: 'rounded-full bg-gray-200',
  avatar: 'rounded-full bg-gray-200 animate-pulse',
  card: 'rounded-lg bg-gray-200 animate-pulse',
} as const;
