import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../core/models/models';

@Component({
  selector: 'app-product-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-management.component.html',
  styleUrls: ['./product-management.component.scss']
})
export class ProductManagementComponent implements OnInit {
  products: Product[] = [];
  loading = false;
  error = '';
  success = '';
  showForm = false;
  readonly compoundingOptions = ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY'];
  readonly payoutOptions = ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY', 'MATURITY'];
  readonly currencies = ['INR', 'USD', 'EUR', 'GBP', 'JPY', 'AED', 'KWD'];

  request = this.emptyRequest();

  constructor(private productService: ProductService) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.productService.getAllProducts().subscribe({
      next: products => {
        this.products = products;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load products.';
        this.loading = false;
      }
    });
  }

  toggleOption(group: 'allowedCompoundingFrequencies' | 'allowedPayoutFrequencies', value: string): void {
    const values = this.request[group];
    this.request[group] = values.includes(value)
      ? values.filter(item => item !== value)
      : [...values, value];
    if (group === 'allowedCompoundingFrequencies' && !this.request[group].includes(this.request.compoundingFrequency)) {
      this.request.compoundingFrequency = this.request[group][0] || '';
    }
  }

  selected(group: 'allowedCompoundingFrequencies' | 'allowedPayoutFrequencies', value: string): boolean {
    return this.request[group].includes(value);
  }

  createProduct(): void {
    this.loading = true;
    this.error = '';
    this.success = '';
    this.productService.createProduct(this.request).subscribe({
      next: () => {
        this.success = `Product ${this.request.productCode} created successfully.`;
        this.request = this.emptyRequest();
        this.showForm = false;
        this.loadProducts();
      },
      error: error => {
        this.error = error?.error?.message || 'Unable to create product. Check all limits and frequency selections.';
        this.loading = false;
      }
    });
  }

  private emptyRequest() {
    return {
      productCode: '',
      productName: '',
      productType: 'FD',
      currency: 'INR',
      effectiveDate: new Date().toISOString().slice(0, 10),
      minTermMonths: 3,
      maxTermMonths: 36,
      minRate: 5,
      maxRate: 7.5,
      minDeposit: 10000,
      maxDeposit: 10000000,
      rateCapAddon: 1.5,
      categoryAddonsStackable: true,
      preMaturityPenaltyPct: 1,
      compoundingFrequency: 'QUARTERLY',
      allowedCompoundingFrequencies: ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY'],
      allowedPayoutFrequencies: ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY', 'MATURITY'],
      dayCountConvention: 'ACTUAL_365',
      prematureClosureAllowed: true
    };
  }
}
