# Finance Assistant - Schematic Wireframes

## 📱 Application Overview
This document contains schematic wireframes for the Finance Assistant application, showing the layout, navigation, and key components of each page.

---

## 🏠 Welcome Page (index.html)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Finance Assistant                        │
│                         Welcome Page                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                    🏠 Finance Assistant                        │
│                                                                 │
│  Welcome to your AI-powered financial management platform.     │
│  Organize your finances, track spending, set goals, and get   │
│  intelligent insights.                                          │
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐                   │
│  │ 🔐 Login /      │    │ 📊 Go to        │                   │
│  │    Register     │    │    Dashboard    │                   │
│  └─────────────────┘    └─────────────────┘                   │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ 📄 Main Pages   │  │ ⚙️ JavaScript   │  │ 🎨 Styles      │ │
│  │ Core application│  │ Application     │  │ CSS files for  │ │
│  │ pages including │  │ logic, API      │  │ consistent     │ │
│  │ dashboard,      │  │ services, and   │  │ design and     │ │
│  │ budget, goals,  │  │ interactive     │  │ responsive     │ │
│  │ transactions    │  │ functionality   │  │ layout         │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ 📚 Documentation│  │ 🧪 Testing      │  │ 📁 Assets      │ │
│  │ README files    │  │ Test pages and  │  │ Additional     │ │
│  │ and technical   │  │ debugging tools │  │ files, scripts,│ │
│  │ documentation   │  │ for development │  │ and resources  │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔐 Authentication Page (html/auth.html)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Finance Assistant                        │
│                      Authentication                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐                   │
│  │ 🔐 Login        │    │ 📝 Register     │                   │
│  └─────────────────┘    └─────────────────┘                   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📧 Email: [________________________]                   │   │
│  │ 🔒 Password: [____________________]                    │   │
│  │                                                         │   │
│  │                    [🔐 Login]                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 👤 Full Name: [________________________]               │   │
│  │ 📧 Email: [________________________]                   │   │
│  │ 🔒 Password: [____________________]                    │   │
│  │ 🔒 Confirm: [____________________]                     │   │
│  │                                                         │   │
│  │                  [📝 Register]                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐                    │
│  │ 💰 Budget       │  │ 🏠 Back to      │                    │
│  │ Management      │  │    Welcome      │                    │
│  └─────────────────┘  └─────────────────┘                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📊 Dashboard (html/dashboard.html)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Finance Assistant                        │
│                          Dashboard                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 👤 Welcome back, [User Name]                           │   │
│  │ 🔐 [Logout]                                            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ 💰 Budget       │  │ 🎯 Goal        │  │ 📤 Transaction │ │
│  │ Management      │  │ Management      │  │ Upload          │ │
│  │ Manage your     │  │ Set and track   │  │ Upload CSV      │ │
│  │ budgets and     │  │ financial       │  │ files with      │ │
│  │ spending        │  │ goals           │  │ transactions    │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ 🧠 AI Insights  │  │ 📋 Transaction │  │                 │ │
│  │ Get intelligent │  │ Management      │  │                 │ │
│  │ financial       │  │ View and edit   │  │                 │ │
│  │ insights        │  │ transactions    │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📊 Quick Overview                                       │   │
│  │                                                         │   │
│  │ 💰 Total Budget: $[Amount]                              │   │
│  │ 💸 Spent: $[Amount]                                     │   │
│  │ 🎯 Active Goals: [Number]                               │   │
│  │ 📊 Recent Transactions: [Number]                        │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 💰 Budget Management (html/budget-management.html)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Finance Assistant                        │
│                      Budget Management                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 💰 Budget Management                                   │   │
│  │ Manage your budgets and track spending across categories│   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ 🏠 Dashboard    │  │ 💰 Budget       │  │ 📤 Transaction │ │
│  │                 │  │ Management      │  │ Upload          │ │
│  │                 │  │ (active)        │  │                 │ │
│  │                 │  │                 │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                 │
│  ┌─────────────────┐                                            │
│  │ 🧠 AI Insights  │                                            │
│  └─────────────────┘                                            │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📊 Budget Overview                                      │   │
│  │                                                         │   │
│  │ Total Budget: $[Amount]                                 │   │
│  │ Total Spent: $[Amount]                                  │   │
│  │ Remaining: $[Amount]                                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 🆕 Create New Budget                                    │   │
│  │                                                         │   │
│  │ Budget Name: [________________________]                 │   │
│  │ Amount: $[________________]                             │   │
│  │ Category: [Dropdown: Food, Transport, etc.]             │   │
│  │ Period: [Dropdown: Monthly, Yearly]                     │   │
│  │                                                         │   │
│  │                    [💾 Save Budget]                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📋 Budget List                                          │   │
│  │                                                         │   │
│  │ ┌─────────────────────────────────────────────────────┐ │   │
│  │ │ 🍽️ Food Budget                    $500 / $600      │ │   │
│  │ │ [Edit] [Delete] [View Details]                     │ │   │
│  │ └─────────────────────────────────────────────────────┘ │   │
│  │                                                         │   │
│  │ ┌─────────────────────────────────────────────────────┐ │   │
│  │ │ 🚗 Transport Budget               $200 / $300       │ │   │
│  │ │ [Edit] [Delete] [View Details]                     │ │   │
│  │ └─────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Goal Management (html/goal-management.html)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Finance Assistant                        │
│                       Goal Management                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 🎯 Goal Management                                     │   │
│  │ Set financial goals and track your progress             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ 🏠 Dashboard    │  │ 💰 Budget       │  │ 📤 Transaction │ │
│  │                 │  │ Management      │  │ Upload          │ │
│  │                 │  │                 │  │                 │ │
│  │                 │  │                 │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                 │
│  ┌─────────────────┐                                            │
│  │ 🧠 AI Insights  │                                            │
│  └─────────────────┘                                            │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 🆕 Create New Goal                                      │   │
│  │                                                         │   │
│  │ Goal Name: [________________________]                   │   │
│  │ Target Amount: $[________________]                      │   │
│  │ Current Amount: $[________________]                     │   │
│  │ Target Date: [Date Picker]                              │   │
│  │ Category: [Dropdown: Savings, Investment, etc.]         │   │
│  │                                                         │   │
│  │                    [💾 Save Goal]                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📋 Goals List                                           │   │
│  │                                                         │   │
│  │ ┌─────────────────────────────────────────────────────┐ │   │
│  │ │ 🏠 Emergency Fund                    $2,000 / $5,000│ │   │
│  │ │ Progress: ████████░░ 40%                            │ │   │
│  │ │ Target: Dec 31, 2024                                │ │   │
│  │ │ [Edit] [Delete] [View Details]                      │ │   │
│  │ └─────────────────────────────────────────────────────┘ │   │
│  │                                                         │   │
│  │ ┌─────────────────────────────────────────────────────┐ │   │
│  │ │ 🚗 New Car                    $8,000 / $15,000      │ │   │
│  │ │ Progress: ████████░░ 53%                            │ │   │
│  │ │ Target: Jun 30, 2025                                │ │   │
│  │ │ [Edit] [Delete] [View Details]                      │ │   │
│  │ └─────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📤 Transaction Upload (html/transaction-upload.html)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Finance Assistant                        │
│                     Transaction Upload                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📤 Transaction Upload                                   │   │
│  │ Upload CSV files to import your financial transactions │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ 🏠 Dashboard    │  │ 💰 Budget       │  │ 📤 Transaction │ │
│  │                 │  │ Management      │  │ Upload          │ │
│  │                 │  │                 │  │ (active)        │ │
│  │                 │  │                 │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                 │
│  ┌─────────────────┐                                            │
│  │ 🧠 AI Insights  │                                            │
│  └─────────────────┘                                            │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📁 File Upload                                          │   │
│  │                                                         │   │
│  │ [Choose File] [No file chosen]                          │   │
│  │                                                         │   │
│  │ Supported formats: CSV                                   │   │
│  │                                                         │   │
│  │                    [📤 Upload Transactions]              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📊 Upload Progress                                      │   │
│  │                                                         │   │
│  │ [Progress Bar: ████████░░ 80%]                          │   │
│  │ Processing: 80 of 100 transactions                      │   │
│  │                                                         │   │
│  │ Status: [Processing/Complete/Error]                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📋 Recent Uploads                                       │   │
│  │                                                         │   │
│  │ ┌─────────────────────────────────────────────────────┐ │   │
│  │ │ transactions_jan.csv         Jan 15, 2024   100 txns│ │   │
│  │ │ [View Details] [Delete]                            │ │   │
│  │ └─────────────────────────────────────────────────────┘ │   │
│  │                                                         │   │
│  │ ┌─────────────────────────────────────────────────────┐ │   │
│  │ │ transactions_dec.csv         Dec 31, 2023    85 txns│ │   │
│  │ │ [View Details] [Delete]                            │ │   │
│  │ └─────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📋 Transaction Management (html/transaction-management.html)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Finance Assistant                        │
│                   Transaction Management                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📋 Transaction Management                               │   │
│  │ View, edit, and manage your financial transactions     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ 🏠 Dashboard    │  │ 💰 Budget       │  │ 📤 Transaction │ │
│  │                 │  │ Management      │  │ Upload          │ │
│  │                 │  │                 │  │                 │ │
│  │                 │  │                 │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐                      │
│  │ 🧠 AI Insights  │  │ 📋 Transaction │                      │
│  │                 │  │ Management      │                      │
│  │                 │  │ (active)        │                      │
│  └─────────────────┘  └─────────────────┘                      │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 🔍 Filters & Search                                     │   │
│  │                                                         │   │
│  │ Date Range: [From] [To]                                 │   │
│  │ Category: [Dropdown] Amount: [Min] [Max]               │   │
│  │ Search: [________________________] [🔍 Search]          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📊 Transaction Summary                                   │   │
│  │                                                         │   │
│  │ Total Transactions: 1,247                               │   │
│  │ Total Amount: $12,450                                   │   │
│  │ Average: $9.98                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📋 Transactions List                                     │   │
│  │                                                         │   │
│  │ ┌─────────────────────────────────────────────────────┐ │   │
│  │ │ Date        Description        Category    Amount   │ │   │
│  │ │ Jan 15     Grocery Store      Food        $45.67   │ │   │
│  │ │ [Edit] [Delete] [View]                            │ │   │
│  │ └─────────────────────────────────────────────────────┘ │   │
│  │                                                         │   │
│  │ ┌─────────────────────────────────────────────────────┐ │   │
│  │ │ Jan 15     Gas Station        Transport   $32.50   │ │   │
│  │ │ [Edit] [Delete] [View]                            │ │   │
│  │ └─────────────────────────────────────────────────────┘ │   │
│  │                                                         │   │
│  │ [◀ Previous] Page 1 of 25 [Next ▶]                     │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🧠 AI Insights (html/insight-service.html)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Finance Assistant                        │
│                         AI Insights                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 🧠 AI Insights                                         │   │
│  │ Get intelligent financial insights and recommendations  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ 🏠 Dashboard    │  │ 💰 Budget       │  │ 📤 Transaction │ │
│  │                 │  │ Management      │  │ Upload          │ │
│  │                 │  │                 │  │                 │ │
│  │                 │  │                 │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                 │
│  ┌─────────────────┐                                            │
│  │ 🧠 AI Insights  │                                            │
│  │ (active)        │                                            │
│  └─────────────────┘                                            │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ ❓ Ask AI Question                                      │   │
│  │                                                         │   │
│  │ [How can I save more money?]                            │   │
│  │                                                         │   │
│  │ [🔍 Generate Insight]                                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 💡 AI Response                                          │   │
│  │                                                         │   │
│  │ Based on your spending patterns, here are some         │   │
│  │ recommendations to save more money:                     │   │
│  │                                                         │   │
│  │ 1. 🍽️ Food spending is 20% above average               │   │
│  │    Consider meal planning and bulk shopping             │   │
│  │                                                         │   │
│  │ 2. 🚗 Transport costs can be reduced by 15%            │   │
│  │    Look into public transport or carpooling            │   │
│  │                                                         │   │
│  │ 3. 💰 You're on track with your emergency fund goal    │   │
│  │    Consider increasing monthly contribution              │   │
│  │                                                         │   │
│  │ [💾 Save Insight] [🔄 New Question]                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 📚 Saved Insights                                       │   │
│  │                                                         │   │
│  │ ┌─────────────────────────────────────────────────────┐ │   │
│  │ │ 💡 Budget Optimization Tips          Jan 15, 2024  │ │   │
│  │ │ [View] [Delete] [Share]                             │ │   │
│  │ └─────────────────────────────────────────────────────┘ │   │
│  │                                                         │   │
│  │ ┌─────────────────────────────────────────────────────┐ │   │
│  │ │ 💡 Investment Strategy               Jan 10, 2024  │ │   │
│  │ │ [View] [Delete] [Share]                             │ │   │
│  │ └─────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🤖 Rule Engine (html/rule-engine.html)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Finance Assistant                        │
│                         Rule Engine                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 🤖 Rule Engine                                         │   │
│  │ Automated financial rules and alerts system             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ 📊 Dashboard    │  │ 💰 Budgets      │  │ 🎯 Goals       │ │
│  │                 │  │                 │  │                 │ │
│  │                 │  │                 │  │                 │ │
│  │                 │  │                 │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ 📤 Upload       │  │ 📋 Transactions│  │ 🧠 AI Insights  │ │
│  │                 │  │                 │  │                 │ │
│  │                 │  │                 │  │                 │ │
│  │                 │  │                 │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 🚧 Coming Soon!                                        │   │
│  │                                                         │   │
│  │ 🛠️ The Rule Engine feature is currently under          │   │
│  │ development.                                            │   │
│  │                                                         │   │
│  │ This will allow you to create automated financial       │   │
│  │ rules, alerts, and automated actions based on your      │   │
│  │ spending patterns and financial goals.                  │   │
│  │                                                         │   │
│  │ Check back soon for updates!                            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────┐                                            │
│  │ 🔐 Logout       │                                            │
│  └─────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🧪 Test Page (html/test.html)

```
┌─────────────────────────────────────────────────────────────────┐
│                        Finance Assistant                        │
│                          Test Page                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 🧪 Finance Assistant Test Page                          │   │
│  │ Test the application setup and file paths               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ ✅ Status Check                                          │   │
│  │                                                         │   │
│  │ CSS Loading: [✓/✗]                                      │   │
│  │ JavaScript Loading: [✓/✗]                               │   │
│  │ Configuration: [✓/✗]                                    │   │
│  │ Authentication: [✓/✗]                                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 🔗 Navigation Test                                      │   │
│  │ Test these links to verify they work:                   │   │
│  │                                                         │   │
│  │ 🏠 Welcome Page                                         │   │
│  │ 🔐 Authentication                                       │   │
│  │ 📊 Dashboard                                            │   │
│  │ 💰 Budget Management                                    │   │
│  │ 🎯 Goal Management                                      │   │
│  │ 📋 Transaction Management                                │   │
│  │ 📤 Transaction Upload                                    │   │
│  │ 🧠 AI Insights                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 🔧 Configuration Test                                   │   │
│  │                                                         │   │
│  │ API Base URL: [Display current value]                   │   │
│  │ Authentication Endpoint: [Display current value]        │   │
│  │                                                         │   │
│  │ [🔄 Refresh Config] [📝 Edit Config]                    │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🧭 Navigation Flow

```
Welcome Page (index.html)
         │
         ├── Login/Register → Auth Page (html/auth.html)
         │         │
         │         └── Success → Dashboard (html/dashboard.html)
         │
         └── Go to Dashboard → Dashboard (html/dashboard.html)
                   │
                   ├── Budget Management → Budget Page (html/budget-management.html)
                   ├── Goal Management → Goal Page (html/goal-management.html)
                   ├── Transaction Upload → Upload Page (html/transaction-upload.html)
                   ├── AI Insights → Insights Page (html/insight-service.html)
                   └── Transaction Management → Transaction Page (html/transaction-management.html)
```

---

## 📱 Responsive Design Considerations

### **Desktop (1200px+)**
- Multi-column layouts
- Side-by-side forms and content
- Hover effects and detailed tooltips

### **Tablet (768px - 1199px)**
- Single-column layouts
- Stacked navigation buttons
- Optimized touch targets

### **Mobile (320px - 767px)**
- Full-width layouts
- Vertical navigation stacks
- Large touch targets (44px minimum)
- Simplified forms and content

---

## 🎨 Design System

### **Colors**
- **Primary**: #667eea (Blue)
- **Secondary**: #718096 (Gray)
- **Success**: #48bb78 (Green)
- **Warning**: #ed8936 (Orange)
- **Error**: #f56565 (Red)
- **Background**: #f7fafc (Light Gray)

### **Typography**
- **Font Family**: Inter (Google Fonts)
- **Headings**: 600-700 weight
- **Body**: 400 weight
- **Captions**: 300 weight

### **Spacing**
- **Small**: 8px
- **Medium**: 16px
- **Large**: 24px
- **Extra Large**: 32px

### **Components**
- **Buttons**: 10px border radius, 15px padding
- **Cards**: 12px border radius, 20px padding
- **Forms**: 6px border radius, 12px padding
- **Navigation**: Consistent spacing and alignment

---

## 📋 Implementation Notes

1. **Navigation Consistency**: All pages use the same navigation bar structure
2. **Responsive Grid**: CSS Grid and Flexbox for responsive layouts
3. **Form Validation**: Client-side validation with visual feedback
4. **Loading States**: Progress indicators and loading spinners
5. **Error Handling**: User-friendly error messages and recovery options
6. **Accessibility**: ARIA labels, keyboard navigation, and screen reader support

---

*This wireframe documentation provides a comprehensive overview of the Finance Assistant application's user interface and navigation structure.* 