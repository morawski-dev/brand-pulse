/**
 * Toast Messages - Centralized toast message definitions
 *
 * All user-facing toast messages for consistency and easy translation
 */

export const toastMessages = {
  // Sentiment Update
  sentimentUpdate: {
    success: (sentiment: string) => `Sentyment został zmieniony na "${sentiment}"`,
    error: 'Nie udało się zaktualizować sentymentu. Spróbuj ponownie.',
    unauthorized: 'Musisz być zalogowany, aby zmienić sentyment',
  },

  // Manual Refresh
  refresh: {
    success: 'Dane zostały odświeżone pomyślnie',
    error: 'Nie udało się odświeżyć danych. Spróbuj ponownie.',
    rateLimit: (timeRemaining: string) =>
      `Odświeżanie możliwe za ${timeRemaining}. Dane są aktualizowane automatycznie codziennie o 3:00.`,
    unauthorized: 'Musisz być zalogowany, aby odświeżyć dane',
    inProgress: 'Trwa pobieranie nowych opinii...',
  },

  // Dashboard Data
  dashboard: {
    loadError: 'Nie udało się załadować danych dashboard. Odśwież stronę.',
    reviewsLoadError: 'Nie udało się załadować opinii. Spróbuj ponownie.',
    summaryLoadError: 'Nie udało się załadować podsumowania. Spróbuj ponownie.',
  },

  // Filters
  filters: {
    invalidDateRange: 'Data końcowa nie może być wcześniejsza niż data początkowa',
    cleared: 'Filtry zostały wyczyszczone',
  },

  // Network
  network: {
    offline: 'Brak połączenia z internetem. Sprawdź swoje połączenie.',
    timeout: 'Żądanie przekroczyło limit czasu. Spróbuj ponownie.',
    serverError: 'Wystąpił błąd serwera. Spróbuj ponownie za chwilę.',
  },

  // Generic
  generic: {
    error: 'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
    success: 'Operacja zakończona sukcesem',
  },
} as const;

/**
 * Helper to get sentiment label in Polish
 */
export function getSentimentLabel(sentiment: string): string {
  const labels: Record<string, string> = {
    POSITIVE: 'pozytywny',
    NEGATIVE: 'negatywny',
    NEUTRAL: 'neutralny',
  };
  return labels[sentiment] || sentiment;
}
