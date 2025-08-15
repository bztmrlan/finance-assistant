# Finance Assistant - Application Mockup Design

## 🎨 Design System

### Color Palette
- **Primary Blue**: `#2563eb` (Navigation, buttons, links)
- **Secondary Teal**: `#0d9488` (Success states, category edit buttons)
- **Accent Green**: `#16a34a` (Add buttons, positive values)
- **Warning Orange**: `#ea580c` (Alerts, warnings)
- **Danger Red**: `#dc2626` (Delete buttons, errors, negative values)
- **Neutral Gray**: `#6b7280` (Secondary text, borders)
- **Light Gray**: `#f3f4f6` (Backgrounds, cards)
- **White**: `#ffffff` (Card backgrounds, text on dark)
- **Dark**: `#1f2937` (Headers, navigation)

### Typography
- **Primary Font**: Inter, system-ui, sans-serif
- **Heading Font**: Inter, system-ui, sans-serif
- **Monospace**: JetBrains Mono, monospace (for amounts, IDs)
- **Font Sizes**:
  - H1: 2rem (32px) - Page titles
  - H2: 1.5rem (24px) - Section headers
  - H3: 1.25rem (20px) - Card titles
  - Body: 1rem (16px) - Regular text
  - Small: 0.875rem (14px) - Secondary text
  - Caption: 0.75rem (12px) - Labels, timestamps

### Spacing System
- **Base Unit**: 4px
- **Spacing Scale**: 4px, 8px, 12px, 16px, 20px, 24px, 32px, 48px, 64px
- **Container Padding**: 24px
- **Card Padding**: 20px
- **Button Padding**: 12px 24px

### Shadows & Elevation
- **Card Shadow**: `0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)`
- **Hover Shadow**: `0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)`
- **Modal Shadow**: `0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)`

---

## 🏠 Dashboard Layout

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Header (Fixed)                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ Navigation Sidebar (Fixed) │ Main Content Area                             │
│                            │                                               │
│ 📊 Dashboard               │ ┌─────────────────────────────────────────────┐ │
│ 💰 Budgets                 │ │ Dashboard Overview                          │ │
│ 💳 Transactions            │ │                                             │ │
│ 🎯 Goals                   │ │ ┌─────────────┐ ┌─────────────┐ ┌─────────┐ │ │
│ 🔍 Insights                │ │ │ Total       │ │ Monthly     │ │ Recent  │ │ │
│ ⚙️ Rules                   │ │ │ Balance     │ │ Spending    │ │ Goals   │ │ │
│ 👤 Profile                 │ │ │ $12,450.00  │ │ $2,340.00   │ │ 3/5     │ │ │
│                            │ │ └─────────────┘ └─────────────┘ └─────────┘ │ │
│                            │ │                                             │ │
│                            │ │ ┌─────────────────────────────────────────┐ │ │
│                            │ │ │ Recent Transactions                     │ │ │
│                            │ │ │ ┌─────┬─────────────┬─────────┬───────┐ │ │ │
│                            │ │ │ │Date │ Description│ Category│ Amount│ │ │ │
│                            │ │ │ ├─────┼─────────────┼─────────┼───────┤ │ │ │
│                            │ │ │ │Today│ Groceries  │ Food    │-$45.20│ │ │ │
│                            │ │ │ │Today│ Gas        │ Fuel    │-$32.15│ │ │
│                            │ │ │ │Today│ Salary     │ Income  │+$2,500│ │ │
│                            │ │ │ └─────┴─────────────┴─────────┴───────┘ │ │
│                            │ │ └─────────────────────────────────────────┘ │ │
│                            │ │                                             │ │
│                            │ │ ┌─────────────────────────────────────────┐ │ │
│                            │ │ │ Budget Status                           │ │ │
│                            │ │ │ ┌─────────────┐ ┌─────────────┐         │ │ │
│                            │ │ │ │ Monthly     │ │ Groceries   │         │ │ │
│                            │ │ │ │ Budget      │ │ Budget      │         │ │ │
│                            │ │ │ │ 75% Used    │ │ 90% Used    │         │ │ │
│                            │ │ │ │ ████████░░  │ │ █████████░  │         │ │
│                            │ │ │ └─────────────┘ └─────────────┘         │ │
│                            │ │ └─────────────────────────────────────────┘ │ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 💰 Budget Management Page

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Header                                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ Navigation │ Budget Management                                             │
│            │                                                               │
│            │ ┌─────────────────────────────────────────────────────────────┐ │
│            │ │ Budgets Overview                    [+ Create New Budget]   │ │
│            │ │                                                             │ │
│            │ │ ┌─────────────────────────────────────────────────────────┐ │ │
│            │ │ │ Monthly Budget - August 2024                            │ │
│            │ │ │ $2,500.00 | Aug 1 - Aug 31 | ACTIVE                    │ │
│            │ │ │                                                         │ │
│            │ │ │ Category Limits:                                        │ │
│            │ │ │ ┌─────────────────────────────────────────────────────┐ │ │
│            │ │ │ │ 🍽️ Food & Dining                    $450.00 / $500.00 │ │
│            │ │ │ │    [✏️] [🗑️]                                   90%  │ │
│            │ │ │ │    █████████░                                      │ │
│            │ │ │ │                                                     │ │
│            │ │ │ │ 🚗 Transportation                   $180.00 / $300.00 │ │
│            │ │ │ │    [✏️] [🗑️]                                   60%  │ │
│            │ │ │ │    ██████░░░░                                      │ │
│            │ │ │ │                                                     │ │
│            │ │ │ │ 🏠 Housing                         $800.00 / $800.00 │ │
│            │ │ │ │    [✏️] [🗑️]                                  100%  │ │
│            │ │ │ │    ██████████                                      │ │
│            │ │ │ │                                                     │ │
│            │ │ │ │ [+ Add Category]                                    │ │
│            │ │ │ └─────────────────────────────────────────────────────┘ │ │
│            │ │ │                                                         │ │
│            │ │ │ [📊 Summary] [✏️ Edit Budget] [🗑️ Archive]            │ │
│            │ │ └─────────────────────────────────────────────────────────┘ │ │
│            │ │                                                             │ │
│            │ │ ┌─────────────────────────────────────────────────────────┐ │ │
│            │ │ │ Quarterly Budget - Q3 2024                              │ │
│            │ │ │ $7,500.00 | Jul 1 - Sep 30 | ACTIVE                    │ │
│            │ │ │                                                         │ │
│            │ │ │ Category Limits:                                        │ │
│            │ │ │ ┌─────────────────────────────────────────────────────┐ │ │
│            │ │ │ │ 🎯 Entertainment                    $200.00 / $500.00 │ │
│            │ │ │ │    [✏️] [🗑️]                                   40%  │ │
│            │ │ │ │    ████░░░░░░                                      │ │
│            │ │ │ │                                                     │ │
│            │ │ │ [+ Add Category]                                    │ │
│            │ │ │ └─────────────────────────────────────────────────────┘ │ │
│            │ │ │                                                         │ │
│            │ │ │ [📊 Summary] [✏️ Edit Budget] [🗑️ Archive]            │ │
│            │ │ └─────────────────────────────────────────────────────────┘ │ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## ✏️ Budget Edit Modal

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ ✕ Edit Budget Details                                    [Update]      │ │
│ ├─────────────────────────────────────────────────────────────────────────┤ │
│ │                                                                         │ │
│ │ Budget Name:                                                            │ │
│ │ [Monthly Budget - August 2024                    ]                      │ │
│ │                                                                         │ │
│ │ Description:                                                             │ │
│ │ [Personal monthly budget for essential expenses and discretionary      ] │ │
│ │ [spending. Focus on food, transportation, and housing costs.          ] │ │
│ │                                                                         │ │
│ │ Start Date:                    End Date:                                │ │
│ │ [2024-08-01]                  [2024-08-31]                             │ │
│ │                                                                         │ │
│ │ Total Budget Amount:                                                    │ │
│ │ [$2,500.00]                                                             │ │
│ │                                                                         │ │
│ │ Budget Period:                                                          │ │
│ │ [Monthly ▼]                                                             │ │
│ │                                                                         │ │
│ │ ┌─────────────────────────────────────────────────────────────────────┐ │ │
│ │ │ ℹ️ Note: To edit category limits, use the edit buttons next to      │ │
│ │ │ each category. To add new categories, use the "Add Category"       │ │
│ │ │ button on the budget card.                                          │ │
│ │ └─────────────────────────────────────────────────────────────────────┘ │ │
│ │                                                                         │ │
│ │ [Cancel]                                                [Update Budget] │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🗑️ Category Delete Confirmation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ ⚠️ Remove Category from Budget                                         │ │
│ ├─────────────────────────────────────────────────────────────────────────┤ │
│ │                                                                         │ │
│ │ Are you sure you want to remove "Food & Dining" from this budget?      │ │
│ │                                                                         │ │
│ │ This will:                                                              │ │
│ │ • Remove the category limit                                            │ │
│ │ • Delete all spending data for this category in this budget            │ │ │
│ │ • This action cannot be undone                                         │ │
│ │                                                                         │ │
│ │                                                                         │ │
│ │ [Cancel]                                                [Remove]        │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## ➕ Add Category Modal

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │ ➕ Add Category to Budget                               [Add Category]  │ │
│ ├─────────────────────────────────────────────────────────────────────────┤ │
│ │                                                                         │ │
│ │ Select Category:                                                        │ │
│ │ [Entertainment ▼]                                                       │
│ │                                                                         │
│ │ Available categories:                                                   │ │
│ │ • 🍽️ Food & Dining                                                     │ │
│ │ • 🚗 Transportation                                                     │ │
│ │ • 🏠 Housing                                                            │ │
│ │ • 🎯 Entertainment                                                      │ │
│ │ • 🛒 Shopping                                                           │ │
│ │ • 💊 Healthcare                                                         │ │
│ │ • 📚 Education                                                          │ │
│ │ • ✈️ Travel                                                             │ │
│ │                                                                         │ │
│ │ Monthly Limit:                                                          │ │
│ │ [$500.00]                                                               │ │
│ │                                                                         │ │
│ │ [Cancel]                                                [Add Category]  │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
```

---

## 💳 Transaction Management Page

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Header                                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ Navigation │ Transaction Management                                       │
│            │                                                               │
│            │ ┌─────────────────────────────────────────────────────────────┐ │
│            │ │ Transactions Overview              [+ Add Transaction]      │ │
│            │ │                                                             │ │
│            │ │ Filters:                                                        │
│            │ │ [All Categories ▼] [All Time ▼] [All Types ▼] [Search...] │ │
│            │ │                                                             │ │
│            │ │ ┌─────────────────────────────────────────────────────────┐ │ │
│            │ │ │ Transaction List                                        │ │
│            │ │ │ ┌─────┬─────────────┬─────────┬─────────┬─────────────┐ │ │
│            │ │ │ │Date │ Description│ Category│ Type    │ Amount      │ │ │
│            │ │ │ ├─────┼─────────────┼─────────┼─────────┼─────────────┤ │ │
│            │ │ │ │Aug 15│ Groceries  │ Food    │ Expense │ -$45.20    │ │ │
│            │ │ │ │Aug 15│ Gas        │ Fuel    │ Expense │ -$32.15    │ │ │
│            │ │ │ │Aug 15│ Salary     │ Income  │ Income  │ +$2,500.00 │ │ │
│            │ │ │ │Aug 14│ Coffee     │ Food    │ Expense │ -$4.50     │ │ │
│            │ │ │ │Aug 14│ Uber       │ Transp. │ Expense │ -$18.75    │ │ │
│            │ │ │ └─────┴─────────────┴─────────┴─────────┴─────────────┘ │ │
│            │ │ │                                                         │ │
│            │ │ │ Showing 1-5 of 127 transactions                         │ │
│            │ │ │ [← Previous] [1] [2] [3] ... [26] [Next →]            │ │
│            │ │ └─────────────────────────────────────────────────────────┘ │ │
│            │ │                                                             │ │
│            │ │ ┌─────────────────────────────────────────────────────────┐ │ │
│            │ │ │ Quick Actions                                          │ │ │
│            │ │ │ [📁 Upload CSV] [📊 Export] [🔍 Advanced Search]       │ │
│            │ │ └─────────────────────────────────────────────────────────┘ │ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Goal Management Page

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Header                                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ Navigation │ Goal Management                                              │
│            │                                                               │
│            │ ┌─────────────────────────────────────────────────────────────┐ │
│            │ │ Financial Goals                            [+ Create Goal]  │ │
│            │ │                                                             │ │
│            │ │ ┌─────────────────────────────────────────────────────────┐ │ │
│            │ │ │ 🏠 Save for House Down Payment                          │ │
│            │ │ │ $15,000 / $50,000 (30%)                                │ │
│            │ │ │ ████████████████████████████████████████████████████████ │ │
│            │ │ │ Target: December 2025 | Monthly: $1,250                │ │
│            │ │ │                                                         │ │
│            │ │ │ [✏️ Edit] [📊 Progress] [🗑️ Delete]                    │ │
│            │ │ └─────────────────────────────────────────────────────────┘ │ │
│            │ │                                                             │ │
│            │ │ ┌─────────────────────────────────────────────────────────┐ │ │
│            │ │ │ 🚗 New Car Fund                                        │ │
│            │ │ │ $8,500 / $25,000 (34%)                                 │ │
│            │ │ │ ████████████████████████████████████████████████████████ │ │
│            │ │ │ Target: June 2025 | Monthly: $1,000                    │ │
│            │ │ │                                                         │ │
│            │ │ │ [✏️ Edit] [📊 Progress] [🗑️ Delete]                    │ │
│            │ │ └─────────────────────────────────────────────────────────┘ │ │
│            │ │                                                             │ │
│            │ │ ┌─────────────────────────────────────────────────────────┐ │ │
│            │ │ │ 💰 Emergency Fund                                      │ │
│            │ │ │ $12,000 / $15,000 (80%)                                │ │
│            │ │ │ ████████████████████████████████████████████████████████ │ │
│            │ │ │ Target: October 2024 | Monthly: $500                   │ │
│            │ │ │                                                         │ │
│            │ │ │ [✏️ Edit] [📊 Progress] [🗑️ Delete]                    │ │
│            │ │ └─────────────────────────────────────────────────────────┘ │ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔍 Insight Service Page

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Header                                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ Navigation │ AI-Powered Insights                                         │
│            │                                                               │
│            │ ┌─────────────────────────────────────────────────────────────┐ │
│            │ │ Financial Insights                                        │ │
│            │ │                                                             │ │
│            │ │ ┌─────────────────────────────────────────────────────────┐ │ │
│            │ │ │ 💡 Spending Pattern Analysis                            │ │
│            │ │ │                                                         │ │
│            │ │ │ Your food spending has increased by 15% this month      │ │
│            │ │ │ compared to last month. Consider reviewing your         │ │
│            │ │ │ grocery budget and meal planning to reduce costs.       │ │
│            │ │ │                                                         │ │
│            │ │ │ [📊 View Details] [💾 Save Insight]                     │ │
│            │ │ └─────────────────────────────────────────────────────────┘ │ │
│            │ │                                                             │ │
│            │ │ ┌─────────────────────────────────────────────────────────┐ │ │
│            │ │ │ 🎯 Budget Optimization                                  │ │
│            │ │ │                                                         │ │
│            │ │ │ Based on your spending patterns, you could save $200    │ │
│            │ │ │ monthly by reducing entertainment expenses and          │ │
│            │ │ │ optimizing your transportation costs.                   │ │
│            │ │ │                                                         │ │
│            │ │ │ [📊 View Details] [💾 Save Insight]                     │ │
│            │ │ └─────────────────────────────────────────────────────────┘ │ │
│            │ │                                                             │ │
│            │ │ ┌─────────────────────────────────────────────────────────┐ │ │
│            │ │ │ 🔮 Future Predictions                                  │ │ │
│            │ │ │                                                         │ │
│            │ │ │ At your current savings rate, you'll reach your house   │ │
│            │ │ │ down payment goal by November 2025, one month early!    │ │
│            │ │ │                                                         │ │
│            │ │ │ [📊 View Details] [💾 Save Insight]                     │ │
│            │ │ └─────────────────────────────────────────────────────────┘ │ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## ⚙️ Rule Engine Page

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Header                                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ Navigation │ Rule Engine                                                  │
│            │                                                               │
│            │ ┌─────────────────────────────────────────────────────────────┐ │
│            │ │ Automated Rules                            [+ Create Rule]  │ │
│            │ │                                                             │ │
│            │ │ ┌─────────────────────────────────────────────────────────┐ │ │
│            │ │ │ 🚨 High Spending Alert                                  │ │
│            │ │ │                                                         │ │
│            │ │ │ IF spending > 80% of budget                             │ │
│            │ │ │ THEN send notification                                  │ │
│            │ │ │                                                         │ │
│            │ │ │ Status: Active | Last Triggered: Aug 15, 2024          │ │
│            │ │ │                                                         │ │
│            │ │ │ [✏️ Edit] [⏸️ Pause] [🗑️ Delete]                       │ │
│            │ │ └─────────────────────────────────────────────────────────┘ │ │
│            │ │                                                             │ │
│            │ │ ┌─────────────────────────────────────────────────────────┐ │ │
│            │ │ │ 💰 Auto-Categorization                                 │ │
│            │ │ │                                                         │ │
│            │ │ │ IF description contains "Starbucks"                     │ │
│            │ │ │ THEN categorize as "Food & Dining"                      │ │
│            │ │ │                                                         │ │
│            │ │ │ Status: Active | Last Triggered: Aug 15, 2024          │ │
│            │ │ │                                                         │ │
│            │ │ │ [✏️ Edit] [⏸️ Pause] [🗑️ Delete]                       │ │
│            │ │ └─────────────────────────────────────────────────────────┘ │ │
│            │ │                                                             │ │
│            │ │ ┌─────────────────────────────────────────────────────────┐ │ │
│            │ │ │ 🎯 Goal Progress Alert                                  │ │ │
│            │ │ │                                                         │ │
│            │ │ │ IF goal progress > 90%                                 │ │
│            │ │ │ THEN send congratulations                               │ │
│            │ │ │                                                         │ │
│            │ │ │ Status: Active | Last Triggered: Aug 10, 2024          │ │
│            │ │ │                                                         │ │
│            │ │ │ [✏️ Edit] [⏸️ Pause] [🗑️ Delete]                       │ │
│            │ │ └─────────────────────────────────────────────────────────┘ │ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔐 Authentication Pages

### Login Page
```
┌─────────────────────────────────────────────────────────────────────────────┐
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │                                                                         │ │
│ │                    💰 Finance Assistant                                │ │
│ │                                                                         │ │
│ │                    Welcome Back!                                       │ │
│ │                                                                         │ │
│ │                    Email:                                              │ │
│ │                    [user@example.com                    ]               │ │
│ │                                                                         │ │
│ │                    Password:                                           │ │
│ │                    [••••••••••••••••••••••••••••••••••••]               │ │
│ │                                                                         │ │
│ │                    [🔒 Remember me]                                    │ │
│ │                                                                         │ │
│ │                    [Sign In]                                           │ │
│ │                                                                         │ │
│ │                    [Forgot Password?]                                  │ │
│ │                                                                         │ │
│ │                    Don't have an account? [Sign Up]                    │ │
│ │                                                                         │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Registration Page
```
┌─────────────────────────────────────────────────────────────────────────────┐
│ ┌─────────────────────────────────────────────────────────────────────────┐ │
│ │                                                                         │ │
│ │                    💰 Finance Assistant                                │ │
│ │                                                                         │ │
│ │                    Create Account                                      │ │
│ │                                                                         │ │
│ │                    Full Name:                                          │ │
│ │                    [John Doe                            ]               │ │
│ │                                                                         │ │
│ │                    Email:                                              │ │
│ │                    [john.doe@example.com                ]               │ │
│ │                                                                         │ │
│ │                    Password:                                           │ │
│ │                    [••••••••••••••••••••••••••••••••••••]               │ │
│ │                                                                         │ │
│ │                    Confirm Password:                                   │ │
│ │                    [••••••••••••••••••••••••••••••••••••]               │ │
│ │                                                                         │ │
│ │                    [Create Account]                                    │ │
│ │                                                                         │ │
│ │                    Already have an account? [Sign In]                  │ │
│ │                                                                         │ │
│ └─────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📱 Responsive Design Considerations

### Mobile Breakpoints
- **Desktop**: 1024px+
- **Tablet**: 768px - 1023px
- **Mobile**: 320px - 767px

### Mobile Adaptations
- **Navigation**: Collapses to hamburger menu
- **Cards**: Stack vertically, full width
- **Tables**: Scroll horizontally or convert to cards
- **Modals**: Full screen on mobile
- **Buttons**: Larger touch targets (44px minimum)

### Touch-Friendly Elements
- **Button Height**: Minimum 44px
- **Spacing**: Minimum 8px between interactive elements
- **Icons**: Minimum 24px × 24px
- **Text**: Minimum 16px for body text

---

## 🎨 Component Specifications

### Buttons
```
Primary Button: [Primary Action]
- Background: #2563eb
- Text: White
- Hover: #1d4ed8
- Active: #1e40af

Secondary Button: [Secondary Action]
- Background: White
- Text: #374151
- Border: #d1d5db
- Hover: #f9fafb

Danger Button: [Delete Action]
- Background: #dc2626
- Text: White
- Hover: #b91c1c
- Active: #991b1b

Success Button: [Add Action]
- Background: #16a34a
- Text: White
- Hover: #15803d
- Active: #166534
```

### Cards
```
Card Container:
- Background: White
- Border: 1px solid #e5e7eb
- Border Radius: 8px
- Shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1)
- Padding: 20px

Card Header:
- Border Bottom: 1px solid #f3f4f6
- Padding Bottom: 16px
- Margin Bottom: 16px

Card Content:
- Line Height: 1.5
- Color: #374151
```

### Forms
```
Input Field:
- Border: 1px solid #d1d5db
- Border Radius: 6px
- Padding: 12px 16px
- Focus: 2px solid #2563eb
- Placeholder: #9ca3af

Label:
- Font Weight: 500
- Color: #374151
- Margin Bottom: 8px

Error State:
- Border: 1px solid #dc2626
- Text: #dc2626
- Background: #fef2f2
```

---

## 🚀 Implementation Notes

### CSS Framework
- Use CSS Grid and Flexbox for layouts
- Implement CSS Custom Properties for theming
- Use CSS-in-JS or CSS Modules for component styling

### Icon System
- Font Awesome or Heroicons for consistent iconography
- SVG icons for custom graphics
- Icon size: 16px, 20px, 24px, 32px

### Animation
- Subtle transitions (200-300ms) for hover states
- Loading spinners for async operations
- Smooth scrolling and page transitions
- Micro-interactions for better UX

### Accessibility
- High contrast ratios (4.5:1 minimum)
- Keyboard navigation support
- Screen reader compatibility
- Focus indicators for all interactive elements
- ARIA labels for complex components

---

*This mockup provides a comprehensive visual guide for implementing the Finance Assistant application with modern, professional design principles and excellent user experience.* 