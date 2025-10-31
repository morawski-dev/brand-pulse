/**
 * Component tests for EmailInput
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EmailInput } from '../EmailInput';

describe('EmailInput', () => {
  const mockOnChange = jest.fn();
  const mockOnBlur = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders with label and input', () => {
    render(
      <EmailInput
        value=""
        onChange={mockOnChange}
        onBlur={mockOnBlur}
      />
    );

    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('twoj@email.com')).toBeInTheDocument();
  });

  it('displays the provided value', () => {
    render(
      <EmailInput
        value="test@example.com"
        onChange={mockOnChange}
        onBlur={mockOnBlur}
      />
    );

    const input = screen.getByLabelText('Email') as HTMLInputElement;
    expect(input.value).toBe('test@example.com');
  });

  it('calls onChange when user types', async () => {
    const user = userEvent.setup();
    render(
      <EmailInput
        value=""
        onChange={mockOnChange}
        onBlur={mockOnBlur}
      />
    );

    const input = screen.getByLabelText('Email');
    await user.type(input, 'test');

    expect(mockOnChange).toHaveBeenCalledTimes(4); // called for each character
  });

  it('calls onBlur when input loses focus', async () => {
    const user = userEvent.setup();
    render(
      <EmailInput
        value=""
        onChange={mockOnChange}
        onBlur={mockOnBlur}
      />
    );

    const input = screen.getByLabelText('Email');
    await user.click(input);
    await user.tab(); // move focus away

    expect(mockOnBlur).toHaveBeenCalledTimes(1);
  });

  it('displays error message when error prop is provided', () => {
    const errorMessage = 'Email jest wymagany';
    render(
      <EmailInput
        value=""
        error={errorMessage}
        onChange={mockOnChange}
        onBlur={mockOnBlur}
      />
    );

    expect(screen.getByText(errorMessage)).toBeInTheDocument();
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });

  it('applies error styling when error is present', () => {
    render(
      <EmailInput
        value=""
        error="Error message"
        onChange={mockOnChange}
        onBlur={mockOnBlur}
      />
    );

    const input = screen.getByLabelText('Email');
    expect(input).toHaveAttribute('aria-invalid', 'true');
  });

  it('does not display error message when no error', () => {
    render(
      <EmailInput
        value="test@example.com"
        onChange={mockOnChange}
        onBlur={mockOnBlur}
      />
    );

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('disables input when disabled prop is true', () => {
    render(
      <EmailInput
        value=""
        onChange={mockOnChange}
        onBlur={mockOnBlur}
        disabled={true}
      />
    );

    const input = screen.getByLabelText('Email');
    expect(input).toBeDisabled();
  });

  it('has correct autocomplete attribute', () => {
    render(
      <EmailInput
        value=""
        onChange={mockOnChange}
        onBlur={mockOnBlur}
      />
    );

    const input = screen.getByLabelText('Email');
    expect(input).toHaveAttribute('autocomplete', 'email');
  });
});
