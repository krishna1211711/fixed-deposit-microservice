import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { FdCalculatorService } from '../../core/services/fd-calculator.service';
import { FdSimulationRequest, FdSimulationResponse } from '../../core/models/models';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format.pipe';

@Component({
  selector: 'app-fd-calculator',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, CurrencyFormatPipe],
  templateUrl: './fd-calculator.component.html',
  styleUrls: ['./fd-calculator.component.scss']
})
export class FdCalculatorComponent implements OnInit {
  request: FdSimulationRequest = {
    principal: 100000,
    termMonths: 12,
    baseRate: 5.5,
    compoundingFrequency: 'QUARTERLY',
    calculationType: 'COMPOUND',
    categories: []
  };

  categoryOptions = [
    { value: 'SENIOR_CITIZEN', label: 'fdCalc.seniorCitizen' },
    { value: 'BANK_EMPLOYEE', label: 'fdCalc.employee' },
    { value: 'PREMIUM_CUSTOMER', label: 'fdCalc.premium' }
  ];

  result: FdSimulationResponse | null = null;
  loading = false;
  error = '';

  constructor(
    private calculatorService: FdCalculatorService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.calculate();
  }

  toggleCategory(event: any, category: string) {
    if (event.target.checked) {
      this.request.categories.push(category);
    } else {
      this.request.categories = this.request.categories.filter(c => c !== category);
    }
  }

  isCategorySelected(category: string): boolean {
    return this.request.categories.includes(category);
  }

  calculate() {
    this.loading = true;
    this.error = '';
    
    this.calculatorService.simulate(this.request).subscribe({
      next: (res) => {
        this.result = res;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'Failed to calculate. Please check inputs.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }
}
