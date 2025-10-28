'use client';

import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import type { PricingCardProps } from '@/lib/types/landing';
import { Check, X } from 'lucide-react';
import { cn } from '@/lib/utils';

/**
 * PricingCard - Single pricing plan card
 * Shows plan name, price, features, limitations, and CTA
 */
export function PricingCard({ plan, onCTAClick }: PricingCardProps) {
  const isHighlighted = plan.highlighted;
  const isFree = plan.price === 0;
  const isComingSoon = plan.ctaAction === 'coming-soon';

  return (
    <Card
      className={cn(
        'relative flex flex-col transition-all duration-300',
        isHighlighted && 'border-primary shadow-lg scale-105',
        !isHighlighted && 'hover:shadow-md'
      )}
    >
      {/* Highlighted badge */}
      {isHighlighted && (
        <div className="absolute -top-3 left-1/2 -translate-x-1/2">
          <Badge className="bg-primary text-primary-foreground">
            Most Popular
          </Badge>
        </div>
      )}

      <CardHeader className="text-center pb-8 pt-8">
        {/* Plan Name */}
        <h3 className="mb-2 text-2xl font-bold text-foreground">
          {plan.name}
        </h3>

        {/* Price */}
        <div className="flex items-baseline justify-center gap-1">
          {typeof plan.price === 'number' ? (
            <>
              <span className="text-4xl font-bold text-foreground">
                ${plan.price}
              </span>
              {plan.period && (
                <span className="text-muted-foreground">/{plan.period}</span>
              )}
            </>
          ) : (
            <span className="text-2xl font-semibold text-muted-foreground">
              Custom Pricing
            </span>
          )}
        </div>
      </CardHeader>

      <CardContent className="flex-1 space-y-6">
        {/* Features List */}
        <div>
          <p className="mb-3 text-sm font-medium text-muted-foreground">
            What's included:
          </p>
          <ul className="space-y-2">
            {plan.features.map((feature, index) => (
              <li key={index} className="flex items-start gap-2">
                <Check className="mt-0.5 h-4 w-4 shrink-0 text-green-600" />
                <span className="text-sm text-foreground">{feature}</span>
              </li>
            ))}
          </ul>
        </div>

        {/* Limitations (if any) */}
        {plan.limitations && plan.limitations.length > 0 && (
          <div>
            <p className="mb-3 text-sm font-medium text-muted-foreground">
              Limitations:
            </p>
            <ul className="space-y-2">
              {plan.limitations.map((limitation, index) => (
                <li key={index} className="flex items-start gap-2">
                  <X className="mt-0.5 h-4 w-4 shrink-0 text-red-500" />
                  <span className="text-sm text-muted-foreground">{limitation}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </CardContent>

      <CardFooter className="pt-6">
        <Button
          className="w-full"
          variant={isHighlighted ? 'default' : 'outline'}
          size="lg"
          disabled={isComingSoon}
          onClick={() => onCTAClick(plan.id, plan.ctaAction)}
        >
          {plan.ctaText}
        </Button>
      </CardFooter>
    </Card>
  );
}