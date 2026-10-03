import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FdAccountService } from '../../../core/services/fd-account.service';
import { ProductService } from '../../../core/services/product.service';
import { Product } from '../../../core/models/models';

@Component({
  selector: 'app-fd-create',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './fd-create.component.html',
  styleUrls: ['./fd-create.component.scss']
})
export class FdCreateComponent implements OnInit {
  private openingIdempotencyKey: string | null = null;
  products: Product[] = [];
  
  request = {
    customerId: '',
    productCode: '',
    principalAmount: null as number | null,
    termMonths: null as number | null,
    branchCode: '',
    currency: 'INR',
    compoundingFrequency: '' as string,
    payoutFrequency: 'MATURITY',
    maturityInstruction: 'PAYOUT'
  };

  selectedProduct: Product | null = null;
  loading = false;
  successMessage = '';
  error = '';

  constructor(
    private fdService: FdAccountService,
    private productService: ProductService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadProducts();
  }

  loadProducts() {
    this.productService.getProducts().subscribe({
      next: (prods) => this.products = prods,
      error: () => this.error = 'Failed to load products'
    });
  }

  onProductChange() {
    this.selectedProduct = this.products.find(p => p.productCode === this.request.productCode) || null;
    if (this.selectedProduct) {
      this.request.currency = this.selectedProduct.currency;
      const compounding = this.selectedProduct.allowedCompoundingFrequencies?.length
        ? this.selectedProduct.allowedCompoundingFrequencies
        : [this.selectedProduct.compoundingFrequency];
      const payouts = this.selectedProduct.allowedPayoutFrequencies?.length
        ? this.selectedProduct.allowedPayoutFrequencies : ['MATURITY'];
      this.selectedProduct.allowedCompoundingFrequencies = compounding;
      this.selectedProduct.allowedPayoutFrequencies = payouts as Product['allowedPayoutFrequencies'];
      this.request.compoundingFrequency = compounding[0];
      this.request.payoutFrequency = payouts.includes('MATURITY' as any) ? 'MATURITY' : payouts[0];
    }
  }

  onSubmit() {
    this.loading = true;
    this.error = '';
    this.successMessage = '';

    this.openingIdempotencyKey ??= crypto.randomUUID();
    this.fdService.createAccount(this.request, this.openingIdempotencyKey).subscribe({
      next: (res) => {
        this.openingIdempotencyKey = null;
        this.loading = false;
        this.successMessage = `Account ${res.fdAccountNo} created successfully!`;
        setTimeout(() => {
          this.router.navigate(['/fd', res.fdAccountNo]);
        }, 2000);
      },
      error: (err) => {
        this.loading = false;
        this.error = 'Failed to create FD account.';
      }
    });
  }
}
