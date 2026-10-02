import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

// Shared UI Components
import { TableComponent } from '@/app/shared/components/table/table';
import { SelectInputComponent } from '@/app/shared/components/select-input/select-input';
import { SegmentedControlComponent } from '@/app/shared/components/segmented-control/segmented-control';
import { ButtonComponent } from '@/app/shared/components/button/button';
import { BaseInputComponent } from '@/app/shared/components/base-input/base-input';

@NgModule({
  declarations: [
    // 1. Declare components/pipes/directives owned by SharedModule
  ],
  imports: [
    // 2. Import Angular/third-party modules needed by those components
    TableComponent,
    SelectInputComponent,
    SegmentedControlComponent,
    ButtonComponent,
    BaseInputComponent,

    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule
  ],
  exports: [
    // 3. RE-EXPORT your components/pipes/directives
    TableComponent,
    SelectInputComponent,
    SegmentedControlComponent,
    ButtonComponent,
    BaseInputComponent,

    // 4. RE-EXPORT common modules so features don't have to import them individually
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule
  ]
})
export class SharedModule { }