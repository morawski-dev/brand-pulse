/**
 * FreePlanInfoBox - Informational box about free plan benefits
 *
 * Displays static information about the FREE plan:
 * - Icon (gift/info)
 * - Text: "Plan darmowy: monitoruj 1 źródło opinii za darmo"
 *
 * Used in registration form to inform users about freemium model
 */

'use client';

import { Gift } from 'lucide-react';
import { Card } from '@/components/ui/card';

// ========================================
// COMPONENT
// ========================================

export function FreePlanInfoBox() {
  return (
    <Card className="border-blue-200 bg-blue-50 p-4">
      <div className="flex items-start gap-3">
        {/* Icon */}
        <Gift className="mt-0.5 h-5 w-5 flex-shrink-0 text-blue-600" />

        {/* Info text */}
        <div className="space-y-1">
          <p className="text-sm font-medium text-blue-900">
            Plan darmowy
          </p>
          <p className="text-sm text-blue-700">
            Monitoruj 1 źródło opinii za darmo. Bez karty kredytowej.
          </p>
        </div>
      </div>
    </Card>
  );
}
