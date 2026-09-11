import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'currencyFormat',
  standalone: true
})
export class CurrencyFormatPipe implements PipeTransform {
  transform(value: number | string, currency: string = 'INR'): string {
    if (value === null || value === undefined || value === '') return '';
    
    const num = typeof value === 'string' ? parseFloat(value) : value;
    if (isNaN(num)) return String(value);

    const localeByCurrency: Record<string, string> = {
      INR: 'en-IN', USD: 'en-US', EUR: 'de-DE', GBP: 'en-GB',
      JPY: 'ja-JP', AED: 'en-AE', KWD: 'en-KW'
    };
    const normalizedCurrency = currency.toUpperCase();
    return new Intl.NumberFormat(localeByCurrency[normalizedCurrency] || 'en-US', {
      style: 'currency',
      currency: normalizedCurrency
    }).format(num);
  }
}
