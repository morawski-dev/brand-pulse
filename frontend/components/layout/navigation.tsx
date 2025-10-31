'use client';

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import type { NavigationProps } from '@/lib/types/landing';
import { Menu, X } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { cn } from '@/lib/utils';

/**
 * Navigation - Top navigation bar with logo, optional links, and auth buttons
 * Features: responsive design, mobile menu, sticky behavior
 */
export function Navigation({ variant = 'default' }: NavigationProps) {
  const router = useRouter();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  // Track scroll position for sticky nav styling
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20);
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Navigation links with smooth scroll
  const scrollToSection = (sectionId: string) => {
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
      setMobileMenuOpen(false);
    }
  };

  const handleSignIn = () => {
    router.push('/login');
  };

  const handleStartFree = () => {
    router.push('/register');
  };

  const handleLogoClick = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const isTransparent = variant === 'transparent' && !scrolled;

  return (
    <nav
      className={cn(
        'sticky top-0 z-40 w-full border-b transition-all duration-300',
        isTransparent
          ? 'border-transparent bg-transparent'
          : 'border-border bg-background/95 backdrop-blur-sm shadow-sm'
      )}
    >
      <div className="container mx-auto px-4 md:px-6">
        <div className="flex h-16 items-center justify-between">
          {/* Logo */}
          <button
            onClick={handleLogoClick}
            className="flex items-center gap-2 transition-opacity hover:opacity-80"
            aria-label="BrandPulse Home"
          >
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
              <span className="text-lg font-bold text-primary-foreground">BP</span>
            </div>
            <span className="text-xl font-bold text-foreground">BrandPulse</span>
          </button>

          {/* Desktop Navigation Links */}
          <div className="hidden items-center gap-6 md:flex">
            <button
              onClick={() => scrollToSection('features-section')}
              className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
            >
              Features
            </button>
            <button
              onClick={() => scrollToSection('demo-section')}
              className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
            >
              Demo
            </button>
            <button
              onClick={() => scrollToSection('pricing-section')}
              className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
            >
              Pricing
            </button>
          </div>

          {/* Desktop Auth Buttons */}
          <div className="hidden items-center gap-3 md:flex">
            <Button
              variant="ghost"
              onClick={handleSignIn}
            >
              Sign In
            </Button>
            <Button onClick={handleStartFree}>
              Start Free
            </Button>
          </div>

          {/* Mobile Menu Button */}
          <button
            className="md:hidden"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle menu"
            aria-expanded={mobileMenuOpen}
          >
            {mobileMenuOpen ? (
              <X className="h-6 w-6 text-foreground" />
            ) : (
              <Menu className="h-6 w-6 text-foreground" />
            )}
          </button>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div className="border-t border-border py-4 md:hidden">
            <div className="flex flex-col gap-4">
              {/* Mobile Navigation Links */}
              <button
                onClick={() => scrollToSection('features-section')}
                className="text-left text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
              >
                Features
              </button>
              <button
                onClick={() => scrollToSection('demo-section')}
                className="text-left text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
              >
                Demo
              </button>
              <button
                onClick={() => scrollToSection('pricing-section')}
                className="text-left text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
              >
                Pricing
              </button>

              {/* Mobile Auth Buttons */}
              <div className="flex flex-col gap-2 pt-4 border-t border-border">
                <Button
                  variant="outline"
                  onClick={handleSignIn}
                  className="w-full"
                >
                  Sign In
                </Button>
                <Button
                  onClick={handleStartFree}
                  className="w-full"
                >
                  Start Free
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </nav>
  );
}