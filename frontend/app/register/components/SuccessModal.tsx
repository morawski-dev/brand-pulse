/**
 * SuccessModal - Registration success confirmation modal
 *
 * Displays after successful registration with:
 * - Success icon (checkmark)
 * - Success message
 * - Auto-redirect to /onboarding after 2 seconds
 * - Optional "Continue" button for immediate redirect
 *
 * Flow:
 * 1. User completes registration
 * 2. Modal appears
 * 3. Auto-redirect after 2s OR user clicks "Continue"
 * 4. Navigate to /onboarding
 */

'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { CheckCircle2 } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { RegisterResponse } from '@/lib/types/auth';

// ========================================
// TYPES
// ========================================

interface SuccessModalProps {
  isOpen: boolean;
  userData: RegisterResponse | null;
  onContinue: () => void;
}

// ========================================
// CONFIGURATION
// ========================================

const AUTO_REDIRECT_DELAY = 2000; // 2 seconds

// ========================================
// COMPONENT
// ========================================

export function SuccessModal({ isOpen, userData, onContinue }: SuccessModalProps) {
  const router = useRouter();

  /**
   * Auto-redirect after 2 seconds
   */
  useEffect(() => {
    if (isOpen) {
      const timer = setTimeout(() => {
        router.push('/onboarding');
      }, AUTO_REDIRECT_DELAY);

      return () => clearTimeout(timer);
    }
  }, [isOpen, router]);

  /**
   * Handle manual continue button click
   */
  const handleContinue = () => {
    onContinue();
    router.push('/onboarding');
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && handleContinue()}>
      <DialogContent className="sm:max-w-md" onEscapeKeyDown={handleContinue}>
        <DialogHeader className="text-center">
          {/* Success icon */}
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
            <CheckCircle2 className="h-10 w-10 text-green-600" aria-hidden="true" />
          </div>

          {/* Title */}
          <DialogTitle className="text-2xl">
            Konto utworzone!
          </DialogTitle>

          {/* Description */}
          <DialogDescription className="text-center">
            Twoje konto zostało pomyślnie utworzone. Za chwilę przekierujemy Cię
            do konfiguracji pierwszego źródła opinii.
          </DialogDescription>
        </DialogHeader>

        {/* Actions */}
        <div className="mt-4 flex justify-center">
          <Button onClick={handleContinue} size="lg" autoFocus>
            Kontynuuj
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
