/**
 * SourceTypeCard component
 * Selectable card for choosing a review source platform
 *
 * Features:
 * - Platform logo/icon
 * - Platform name and description
 * - "Zalecane" badge for Google
 * - Selected state styling
 * - Hover effects
 * - Keyboard accessible
 */

'use client';

import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { Check } from 'lucide-react';
import { SourceType } from '@/lib/types/onboarding';
import { cn } from '@/lib/utils';

// ========================================
// TYPES
// ========================================

export interface SourceTypeCardProps {
  /**
   * Source type (Google/Facebook/Trustpilot)
   */
  sourceType: SourceType;

  /**
   * Display name (e.g., "Google")
   */
  displayName: string;

  /**
   * Short description
   */
  description: string;

  /**
   * Whether this source is recommended
   */
  isRecommended?: boolean;

  /**
   * Whether this card is currently selected
   */
  isSelected: boolean;

  /**
   * Callback when card is clicked
   */
  onSelect: () => void;

  /**
   * Whether card is disabled
   */
  disabled?: boolean;
}

// ========================================
// PLATFORM ICONS
// ========================================

/**
 * Platform icon components
 * Using simple SVG shapes as placeholders
 * TODO: Replace with actual brand logos
 */
function GoogleIcon({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="10" fill="#4285F4" />
      <path
        d="M12 7v5l4 2"
        stroke="white"
        strokeWidth="2"
        strokeLinecap="round"
      />
    </svg>
  );
}

function FacebookIcon({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="10" fill="#1877F2" />
      <path
        d="M14 8h2V6h-2c-1.1 0-2 .9-2 2v2H10v2h2v6h2v-6h2l.5-2H14V8z"
        fill="white"
      />
    </svg>
  );
}

function TrustpilotIcon({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="10" fill="#00B67A" />
      <path
        d="M12 6l1.5 4.5h4.5l-3.5 2.5 1.5 4.5L12 15l-3.5 2.5 1.5-4.5-3.5-2.5h4.5L12 6z"
        fill="white"
      />
    </svg>
  );
}

/**
 * Get icon component for source type
 */
function getSourceIcon(sourceType: SourceType) {
  switch (sourceType) {
    case SourceType.GOOGLE:
      return GoogleIcon;
    case SourceType.FACEBOOK:
      return FacebookIcon;
    case SourceType.TRUSTPILOT:
      return TrustpilotIcon;
    default:
      return GoogleIcon;
  }
}

// ========================================
// COMPONENT
// ========================================

/**
 * Source type selection card
 *
 * @example
 * ```tsx
 * <SourceTypeCard
 *   sourceType={SourceType.GOOGLE}
 *   displayName="Google"
 *   description="Opinie z Google Maps"
 *   isRecommended={true}
 *   isSelected={selectedType === SourceType.GOOGLE}
 *   onSelect={() => setSelectedType(SourceType.GOOGLE)}
 * />
 * ```
 */
export function SourceTypeCard({
  sourceType,
  displayName,
  description,
  isRecommended = false,
  isSelected,
  onSelect,
  disabled = false,
}: SourceTypeCardProps) {
  const IconComponent = getSourceIcon(sourceType);

  // ========================================
  // HANDLERS
  // ========================================

  const handleClick = () => {
    if (!disabled) {
      onSelect();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (disabled) return;

    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onSelect();
    }
  };

  // ========================================
  // RENDER
  // ========================================

  return (
    <Card
      role="radio"
      aria-checked={isSelected}
      aria-label={`${displayName} - ${description}`}
      tabIndex={disabled ? -1 : 0}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      className={cn(
        'relative cursor-pointer transition-all duration-200',
        'p-6 hover:shadow-lg',
        {
          // Selected state
          'border-2 border-blue-600 bg-blue-50 shadow-md': isSelected,
          // Not selected state
          'border-2 border-transparent hover:border-gray-300': !isSelected,
          // Disabled state
          'cursor-not-allowed opacity-50': disabled,
        }
      )}
    >
      {/* Selected checkmark */}
      {isSelected && (
        <div className="absolute right-3 top-3">
          <div className="flex h-6 w-6 items-center justify-center rounded-full bg-blue-600">
            <Check className="h-4 w-4 text-white" aria-hidden="true" />
          </div>
        </div>
      )}

      {/* Card content */}
      <div className="space-y-3">
        {/* Icon and badge row */}
        <div className="flex items-start justify-between gap-2">
          <IconComponent className="h-12 w-12 flex-shrink-0" />
          {isRecommended && (
            <Badge
              variant="secondary"
              className="bg-green-100 text-green-700 hover:bg-green-100"
            >
              Zalecane
            </Badge>
          )}
        </div>

        {/* Platform name */}
        <h3 className="text-lg font-semibold text-gray-900">{displayName}</h3>

        {/* Description */}
        <p className="text-sm text-gray-600">{description}</p>
      </div>

      {/* Focus ring for keyboard navigation */}
      <div
        className={cn(
          'absolute inset-0 rounded-lg ring-2 ring-blue-600 ring-offset-2',
          'pointer-events-none opacity-0 transition-opacity',
          'focus-within:opacity-100'
        )}
        aria-hidden="true"
      />
    </Card>
  );
}
