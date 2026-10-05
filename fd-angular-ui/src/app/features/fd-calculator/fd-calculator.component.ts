import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { FdCalculatorService } from '../../core/services/fd-calculator.service';
import { FdSimulationRequest, FdSimulationResponse, Product } from '../../core/models/models';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format.pipe';
import { ProductService } from '../../core/services/product.service';

@Component({
  selector: 'app-fd-calculator',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule, CurrencyFormatPipe],
  templateUrl: './fd-calculator.component.html',
  styleUrls: ['./fd-calculator.component.scss']
})
export class FdCalculatorComponent implements OnInit {
  request: FdSimulationRequest = {
    productCode: '',
    principal: 100000,
    termMonths: 12,
    compoundingFrequency: 'QUARTERLY'
  };

  products: Product[] = [];

  result: FdSimulationResponse | null = null;
  loading = false;
  error = '';

  constructor(
    private calculatorService: FdCalculatorService,
    private productService: ProductService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.productService.getProducts('FD', 'ACTIVE').subscribe(products => {
      this.products = products;
      if (products.length > 0) {
        this.request.productCode = products[0].productCode;
        this.request.compoundingFrequency = products[0].compoundingFrequency;
        this.calculate();
      }
    });
  }

  get selectedProduct(): Product | undefined {
    return this.products.find(product => product.productCode === this.request.productCode);
  }

  productChanged() {
    const product = this.selectedProduct;
    if (product) this.request.compoundingFrequency = product.compoundingFrequency;
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
