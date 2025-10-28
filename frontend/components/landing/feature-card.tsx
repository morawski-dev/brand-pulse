'use client';

import { Card, CardContent } from '@/components/ui/card';
import type { FeatureCardProps } from '@/lib/types/landing';
import { cn } from '@/lib/utils';
import * as Icons from 'lucide-react';

/**
 * FeatureCard - Single feature card with icon, title, and description
 * Includes hover effects (scale, shadow)
 */
export function FeatureCard({ feature, index = 0 }: FeatureCardProps) {
  // Dynamically get the icon component from lucide-react
  const Icon = (Icons as any)[feature.icon] || Icons.HelpCircle;

  // Calculate animation delay for staggered entrance
  const animationDelay = index * 100; // 100ms between each card

  return (
    <Card
      className={cn(
        'group relative overflow-hidden border-border/50 transition-all duration-300',
        'hover:scale-[1.02] hover:shadow-lg hover:border-primary/20',
        'animate-in fade-in slide-in-from-bottom-4'
      )}
      style={{
        animationDelay: `${animationDelay}ms`,
        animationFillMode: 'backwards'
      }}
    >
      <CardContent className="flex flex-col items-start gap-4 p-6">
        {/* Icon */}
        <div className="rounded-lg bg-primary/10 p-3 transition-colors group-hover:bg-primary/15">
          <Icon className="h-6 w-6 text-primary" />
        </div>

        {/* Title */}
        <h3 className="text-xl font-semibold tracking-tight text-foreground">
          {feature.title}
        </h3>

        {/* Description */}
        <p className="text-sm leading-relaxed text-muted-foreground">
          {feature.description}
        </p>
      </CardContent>
    </Card>
  );
}