/**
 * Register page layout - without navigation bar
 * Overrides root layout for clean registration flow
 */

export default function RegisterLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
