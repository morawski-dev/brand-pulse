import type { Metadata } from "next";
import { Navigation } from "@/components/layout/navigation";
import "./globals.css";

export const metadata: Metadata = {
  metadataBase: new URL('https://brandpulse.io'),
  title: "BrandPulse - Monitor Customer Reviews in One Place",
  description: "Aggregate and analyze reviews from Google, Facebook and Trustpilot. AI sentiment analysis, unified dashboard, smart filters. Free plan for 1 review source.",
  keywords: [
    "review management",
    "customer feedback",
    "sentiment analysis",
    "Google reviews",
    "Facebook reviews",
    "Trustpilot",
    "review aggregation",
    "business reputation",
    "review monitoring"
  ],
  authors: [{ name: "BrandPulse" }],
  creator: "BrandPulse",
  publisher: "BrandPulse",
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      "max-video-preview": -1,
      "max-image-preview": "large",
      "max-snippet": -1,
    },
  },
  openGraph: {
    type: "website",
    locale: "en_US",
    url: "https://brandpulse.io",
    siteName: "BrandPulse",
    title: "BrandPulse - Monitor Customer Reviews in One Place",
    description: "Aggregate and analyze reviews from Google, Facebook and Trustpilot with AI-powered sentiment analysis.",
    images: [
      {
        url: "/og-image.png",
        width: 1200,
        height: 630,
        alt: "BrandPulse - Review Management Platform",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "BrandPulse - Monitor Customer Reviews in One Place",
    description: "Aggregate and analyze reviews from Google, Facebook and Trustpilot with AI-powered sentiment analysis.",
    images: ["/og-image.png"],
    creator: "@brandpulse",
  },
  alternates: {
    canonical: "https://brandpulse.io",
  },
  category: "Business",
  applicationName: "BrandPulse",
  formatDetection: {
    email: false,
    address: false,
    telephone: false,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        {/* Structured Data (JSON-LD) */}
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{
            __html: JSON.stringify({
              "@context": "https://schema.org",
              "@type": "SoftwareApplication",
              name: "BrandPulse",
              applicationCategory: "BusinessApplication",
              operatingSystem: "Web",
              description: "Review management and sentiment analysis platform that aggregates customer feedback from multiple sources.",
              offers: {
                "@type": "Offer",
                price: "0",
                priceCurrency: "USD",
                category: "Free"
              },
              aggregateRating: {
                "@type": "AggregateRating",
                ratingValue: "4.8",
                ratingCount: "127"
              },
              featureList: [
                "Review Aggregation from Google, Facebook, Trustpilot",
                "AI-powered Sentiment Analysis",
                "Unified Dashboard",
                "Smart Filtering",
                "Daily Automatic Sync",
                "Multi-location Support"
              ]
            })
          }}
        />
      </head>
      <body className="antialiased">
        <Navigation variant="default" />
        {children}
      </body>
    </html>
  );
}