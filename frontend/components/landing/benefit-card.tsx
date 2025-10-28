'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import type { BenefitCardProps } from '@/lib/types/landing';
import { cn } from '@/lib/utils';
import * as Icons from 'lucide-react';

// Target audience label mapping
const TARGET_AUDIENCE_LABELS = {
  'small-business': 'For Small Businesses',
  'chain': 'For Chains',
  'all': 'For Everyone'
} as const;

/**
 * BenefitCard - Single benefit card with icon, title, description, and target audience badge
 */
export function BenefitCard({ benefit }: BenefitCardProps) {
  // Dynamically get the icon component from lucide-react if provided
  const Icon = benefit.icon ? ((Icons as any)[benefit.icon] || null) : null;

  return (
    <Card
      className={cn(
        'group relative overflow-hidden border-border/50 transition-all duration-300',
        'hover:scale-[1.01] hover:shadow-md hover:border-primary/20'
      )}
    >
      <CardContent className="flex flex-col gap-4 p-6">
        {/* Header with icon and badge */}
        <div className="flex items-start justify-between gap-4">
          {Icon && (
            <div className="shrink-0 rounded-lg bg-primary/10 p-3">
              <Icon className="h-6 w-6 text-primary" />
            </div>
          )}
          <Badge variant="secondary" className="shrink-0">
            {TARGET_AUDIENCE_LABELS[benefit.targetAudience]}
          </Badge>
        </div>

        {/* Title */}
        <h3 className="text-2xl font-semibold tracking-tight text-foreground">
          {benefit.title}
        </h3>

        {/* Description */}
        <p className="text-base leading-relaxed text-muted-foreground">
          {benefit.description}
        </p>
      </CardContent>
    </Card>
  );
}