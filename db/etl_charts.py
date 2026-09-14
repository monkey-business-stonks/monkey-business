"""
ETL Pipeline for Financial Data
Extracts, transforms, and loads mock financial data, then generates visualizations
"""

import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime
import os

# Set style for better-looking plots
sns.set_style("darkgrid")
sns.set_palette("husl")
plt.rcParams['figure.figsize'] = (14, 8)
plt.rcParams['font.size'] = 11
plt.rcParams['axes.labelsize'] = 12
plt.rcParams['axes.titlesize'] = 14
plt.rcParams['xtick.labelsize'] = 11
plt.rcParams['ytick.labelsize'] = 11
plt.rcParams['legend.fontsize'] = 10
plt.rcParams['figure.titlesize'] = 16

# ============================================================================
# EXTRACT: Load mock data
# ============================================================================

# Users data
users_data = [
    ('11111111-1111-1111-1111-111111111111', 'user01', 'Alice Kim', 'alice.kim@example.com', 'USER'),
    ('22222222-2222-2222-2222-222222222222', 'user02', 'Brian Lee', 'brian.lee@example.com', 'USER'),
    ('33333333-3333-3333-3333-333333333333', 'user03', 'Carla Ruiz', 'carla.ruiz@example.com', 'USER'),
    ('44444444-4444-4444-4444-444444444444', 'user04', 'David Chen', 'david.chen@example.com', 'USER'),
    ('55555555-5555-5555-5555-555555555555', 'user05', 'Emma Singh', 'emma.singh@example.com', 'USER'),
    ('66666666-6666-6666-6666-666666666666', 'analyst01', 'Frank Moore', 'frank.moore@example.com', 'ANALYST'),
    ('77777777-7777-7777-7777-777777777777', 'analyst02', 'Grace Hill', 'grace.hill@example.com', 'ANALYST'),
    ('88888888-8888-8888-8888-888888888888', 'ops01', 'Henry Park', 'henry.park@example.com', 'OPERATIONS'),
    ('99999999-9999-9999-9999-999999999999', 'ops02', 'Ivy Brooks', 'ivy.brooks@example.com', 'OPERATIONS'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'user06', 'Jake Ford', 'jake.ford@example.com', 'USER'),
]

# Accounts data
accounts_data = [
    ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '11111111-1111-1111-1111-111111111111', 'BROKERAGE', 1200.00, 3500.00),
    ('aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '11111111-1111-1111-1111-111111111111', 'CRYPTO', 500.00, 1800.00),
    ('aaaaaaa3-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '22222222-2222-2222-2222-222222222222', 'BROKERAGE', 800.00, 2400.00),
    ('aaaaaaa4-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '22222222-2222-2222-2222-222222222222', 'FOREX', 1500.00, 1500.00),
    ('aaaaaaa5-aaaa-aaaa-aaaa-aaaaaaaaaaa5', '33333333-3333-3333-3333-333333333333', 'BROKERAGE', 2000.00, 4200.00),
    ('aaaaaaa6-aaaa-aaaa-aaaa-aaaaaaaaaaa6', '33333333-3333-3333-3333-333333333333', '401K', 9000.00, 15000.00),
    ('aaaaaaa7-aaaa-aaaa-aaaa-aaaaaaaaaaa7', '44444444-4444-4444-4444-444444444444', 'BROKERAGE', 300.00, 900.00),
    ('aaaaaaa8-aaaa-aaaa-aaaa-aaaaaaaaaaa8', '44444444-4444-4444-4444-444444444444', 'CRYPTO', 700.00, 2500.00),
    ('aaaaaaa9-aaaa-aaaa-aaaa-aaaaaaaaaaa9', '55555555-5555-5555-5555-555555555555', 'BROKERAGE', 500.00, 2000.00),
    ('aaaaaa10-aaaa-aaaa-aaaa-aaaaaaaaaaa0', '55555555-5555-5555-5555-555555555555', 'ROTH IRA', 4000.00, 8000.00),
    ('aaaaaa11-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '66666666-6666-6666-6666-666666666666', 'BROKERAGE', 1000.00, 3500.00),
    ('aaaaaa12-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '66666666-6666-6666-6666-666666666666', 'FOREX', 2000.00, 2000.00),
    ('aaaaaa13-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '77777777-7777-7777-7777-777777777777', 'BROKERAGE', 600.00, 2600.00),
    ('aaaaaa14-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '77777777-7777-7777-7777-777777777777', 'CRYPTO', 300.00, 900.00),
    ('aaaaaa15-aaaa-aaaa-aaaa-aaaaaaaaaaa5', '88888888-8888-8888-8888-888888888888', 'BROKERAGE', 1500.00, 4500.00),
    ('aaaaaa16-aaaa-aaaa-aaaa-aaaaaaaaaaa6', '88888888-8888-8888-8888-888888888888', '401K', 12000.00, 20000.00),
    ('aaaaaa17-aaaa-aaaa-aaaa-aaaaaaaaaaa7', '99999999-9999-9999-9999-999999999999', 'BROKERAGE', 700.00, 2100.00),
    ('aaaaaa18-aaaa-aaaa-aaaa-aaaaaaaaaaa8', '99999999-9999-9999-9999-999999999999', 'CRYPTO', 400.00, 1400.00),
    ('aaaaaa19-aaaa-aaaa-aaaa-aaaaaaaaaaa9', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'BROKERAGE', 900.00, 3000.00),
    ('aaaaaa20-aaaa-aaaa-aaaa-aaaaaaaaaa10', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'FOREX', 500.00, 500.00),
]

# Assets data
assets_data = [
    ('bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'EQUITY', 'AAPL', 'Apple Inc.', 150, 10),
    ('bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'EQUITY', 'TSLA', 'Tesla Inc.', 200, 5),
    ('bbbbbbb3-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'CRYPTO', 'BTC', 'Bitcoin', 30000, 0.03),
    ('bbbbbbb4-bbbb-bbbb-bbbb-bbbbbbbbbbb4', 'aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'CRYPTO', 'ETH', 'Ethereum', 2000, 1.2),
    ('bbbbbbb5-bbbb-bbbb-bbbb-bbbbbbbbbbb5', 'aaaaaaa3-aaaa-aaaa-aaaa-aaaaaaaaaaa3', 'EQUITY', 'MSFT', 'Microsoft Corp.', 280, 8),
    ('bbbbbbb6-bbbb-bbbb-bbbb-bbbbbbbbbbb6', 'aaaaaaa3-aaaa-aaaa-aaaa-aaaaaaaaaaa3', 'EQUITY', 'AMZN', 'Amazon.com Inc.', 110, 15),
    ('bbbbbbb7-bbbb-bbbb-bbbb-bbbbbbbbbbb7', 'aaaaaaa4-aaaa-aaaa-aaaa-aaaaaaaaaaa4', 'FOREX', 'EURUSD', 'Euro/USD', 1.10, 5000),
    ('bbbbbbb8-bbbb-bbbb-bbbb-bbbbbbbbbbb8', 'aaaaaaa4-aaaa-aaaa-aaaa-aaaaaaaaaaa4', 'FOREX', 'GBPUSD', 'British Pound/USD', 1.25, 3000),
    ('bbbbbbb9-bbbb-bbbb-bbbb-bbbbbbbbbbb9', 'aaaaaaa5-aaaa-aaaa-aaaa-aaaaaaaaaaa5', 'EQUITY', 'NVDA', 'NVIDIA Corp.', 400, 4),
    ('bbbbbb10-bbbb-bbbb-bbbb-bbbbbbbbbbb0', 'aaaaaaa5-aaaa-aaaa-aaaa-aaaaaaaaaaa5', 'EQUITY', 'META', 'Meta Platforms', 250, 6),
    ('bbbbbb11-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'aaaaaaa6-aaaa-aaaa-aaaa-aaaaaaaaaaa6', 'EQUITY', 'AAPL', 'Apple Inc.', 140, 20),
    ('bbbbbb12-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'aaaaaaa6-aaaa-aaaa-aaaa-aaaaaaaaaaa6', 'EQUITY', 'MSFT', 'Microsoft Corp.', 300, 12),
    ('bbbbbb13-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'aaaaaaa7-aaaa-aaaa-aaaa-aaaaaaaaaaa7', 'EQUITY', 'TSLA', 'Tesla Inc.', 210, 3),
    ('bbbbbb14-bbbb-bbbb-bbbb-bbbbbbbbbbb4', 'aaaaaaa7-aaaa-aaaa-aaaa-aaaaaaaaaaa7', 'EQUITY', 'AMZN', 'Amazon.com Inc.', 115, 7),
    ('bbbbbb15-bbbb-bbbb-bbbb-bbbbbbbbbbb5', 'aaaaaaa8-aaaa-aaaa-aaaa-aaaaaaaaaaa8', 'CRYPTO', 'SOL', 'Solana', 90, 10),
    ('bbbbbb16-bbbb-bbbb-bbbb-bbbbbbbbbbb6', 'aaaaaaa8-aaaa-aaaa-aaaa-aaaaaaaaaaa8', 'CRYPTO', 'ETH', 'Ethereum', 2100, 0.8),
]

# Orders data
orders_data = [
    ('ccccccc1-cccc-cccc-cccc-ccccccccccc1', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'EQUITY', 'BUY', 'AAPL', 5, 'SUBMITTED'),
    ('ccccccc2-cccc-cccc-cccc-ccccccccccc2', 'aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'EQUITY', 'SELL', 'TSLA', 2, 'FILLED'),
    ('ccccccc3-cccc-cccc-cccc-ccccccccccc3', 'aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'CRYPTO', 'BUY', 'BTC', 0.01, 'ACCEPTED'),
    ('ccccccc4-cccc-cccc-cccc-ccccccccccc4', 'aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'CRYPTO', 'SELL', 'ETH', 0.5, 'FILLED'),
    ('ccccccc5-cccc-cccc-cccc-ccccccccccc5', 'aaaaaaa3-aaaa-aaaa-aaaa-aaaaaaaaaaa3', 'EQUITY', 'BUY', 'MSFT', 3, 'SUBMITTED'),
    ('ccccccc6-cccc-cccc-cccc-ccccccccccc6', 'aaaaaaa3-aaaa-aaaa-aaaa-aaaaaaaaaaa3', 'EQUITY', 'SELL', 'AMZN', 4, 'FILLED'),
    ('ccccccc7-cccc-cccc-cccc-ccccccccccc7', 'aaaaaaa4-aaaa-aaaa-aaaa-aaaaaaaaaaa4', 'FOREX', 'BUY', 'EURUSD', 1000, 'REJECTED'),
    ('ccccccc8-cccc-cccc-cccc-ccccccccccc8', 'aaaaaaa4-aaaa-aaaa-aaaa-aaaaaaaaaaa4', 'FOREX', 'SELL', 'GBPUSD', 500, 'FILLED'),
    ('ccccccc9-cccc-cccc-cccc-ccccccccccc9', 'aaaaaaa5-aaaa-aaaa-aaaa-aaaaaaaaaaa5', 'EQUITY', 'BUY', 'NVDA', 2, 'SUBMITTED'),
    ('cccccc10-cccc-cccc-cccc-ccccccccccc0', 'aaaaaaa5-aaaa-aaaa-aaaa-aaaaaaaaaaa5', 'EQUITY', 'SELL', 'META', 3, 'FILLED'),
]

# Create DataFrames
df_users = pd.DataFrame(users_data, columns=['userId', 'username', 'name', 'email', 'accessLevel'])
df_accounts = pd.DataFrame(accounts_data, columns=['accountId', 'userId', 'accountType', 'cashBalance', 'balance'])
df_assets = pd.DataFrame(assets_data, columns=['assetId', 'accountId', 'assetClass', 'ticker', 'name', 'averageCost', 'quantity'])
df_orders = pd.DataFrame(orders_data, columns=['orderId', 'accountId', 'orderType', 'action', 'ticker', 'quantity', 'status'])

# ============================================================================
# TRANSFORM: Process and aggregate data
# ============================================================================

# Merge accounts with users
df_accounts_users = df_accounts.merge(df_users, on='userId')

# Calculate total balance by user
df_user_totals = df_accounts_users.groupby('name').agg({
    'balance': 'sum',
    'cashBalance': 'sum'
}).reset_index().sort_values('balance', ascending=False)

# Calculate asset holdings value
df_assets['totalValue'] = df_assets['averageCost'] * df_assets['quantity']

# Merge assets with accounts and users
# First rename to avoid confusion
df_accounts_users_renamed = df_accounts_users.copy()
df_accounts_users_renamed.rename(columns={'name': 'userName'}, inplace=True)
df_assets_full = df_assets.merge(df_accounts_users_renamed[['accountId', 'userName', 'accountType']], on='accountId')

# Rename asset name column for clarity
df_assets_full.rename(columns={'name': 'assetName'}, inplace=True)

# Asset class distribution
df_asset_class = df_assets_full.groupby('assetClass')['totalValue'].sum().reset_index().sort_values('totalValue', ascending=False)

# Account type distribution
df_account_type = df_accounts.groupby('accountType').agg({
    'balance': 'sum',
    'accountId': 'count'
}).reset_index()
df_account_type.columns = ['accountType', 'totalBalance', 'accountCount']

# Order status distribution
df_order_status = df_orders.groupby('status').size().reset_index(name='count')

# User access level distribution
df_access_level = df_users.groupby('accessLevel').size().reset_index(name='count')

# Top holdings by value
df_top_holdings = df_assets_full.nlargest(10, 'totalValue')[['ticker', 'assetName', 'quantity', 'averageCost', 'totalValue']]

# ============================================================================
# LOAD & VISUALIZE: Generate Charts
# ============================================================================

# Create output directory if it doesn't exist
output_dir = '/home/ec2-user/repos/monkey-business/db/charts'
os.makedirs(output_dir, exist_ok=True)

# ============================================================================
# Chart 1: User Portfolio Values
# ============================================================================
fig, ax = plt.subplots(figsize=(14, 8))
colors_users = sns.color_palette("viridis", len(df_user_totals))
bars = ax.barh(df_user_totals['name'], df_user_totals['balance'], color=colors_users, edgecolor='black', linewidth=1.5)
ax.set_xlabel('Total Balance ($)', fontsize=13, fontweight='bold')
ax.set_ylabel('User Name', fontsize=13, fontweight='bold')
ax.set_title('User Portfolio Values', fontsize=15, fontweight='bold', pad=20)
ax.grid(axis='x', alpha=0.4, linestyle='--')

# Add value labels on bars
for i, bar in enumerate(bars):
    width = bar.get_width()
    ax.text(width, bar.get_y() + bar.get_height()/2., f' ${width:,.0f}',
            ha='left', va='center', fontweight='bold', fontsize=10)

plt.tight_layout()
plt.savefig(f'{output_dir}/01_user_portfolio_values.png', dpi=300, bbox_inches='tight', facecolor='white')
print(f"✓ Saved: {output_dir}/01_user_portfolio_values.png")
plt.close()

# ============================================================================
# Chart 2: Account Type Distribution (Pie Chart)
# ============================================================================
fig, ax = plt.subplots(figsize=(12, 8))
colors_account = sns.color_palette("Set2", len(df_account_type))
wedges, texts, autotexts = ax.pie(
    df_account_type['totalBalance'],
    labels=df_account_type['accountType'],
    autopct='%1.1f%%',
    colors=colors_account,
    startangle=90,
    textprops={'fontsize': 12, 'weight': 'bold'},
    explode=[0.1] * len(df_account_type),
    wedgeprops={'edgecolor': 'black', 'linewidth': 2}
)
ax.set_title('Portfolio Distribution by Account Type', fontsize=15, fontweight='bold', pad=20)

# Make percentage text more readable
for autotext in autotexts:
    autotext.set_color('white')
    autotext.set_fontsize(11)
    autotext.set_weight('bold')

plt.tight_layout()
plt.savefig(f'{output_dir}/02_account_type_distribution.png', dpi=300, bbox_inches='tight', facecolor='white')
print(f"✓ Saved: {output_dir}/02_account_type_distribution.png")
plt.close()

# ============================================================================
# Chart 3: Asset Class Distribution (Bar Chart)
# ============================================================================
fig, ax = plt.subplots(figsize=(12, 7))
colors_asset = sns.color_palette("coolwarm", len(df_asset_class))
bars = ax.bar(df_asset_class['assetClass'], df_asset_class['totalValue'], 
              color=colors_asset, edgecolor='black', linewidth=2, width=0.6)
ax.set_ylabel('Total Value ($)', fontsize=13, fontweight='bold')
ax.set_xlabel('Asset Class', fontsize=13, fontweight='bold')
ax.set_title('Asset Class Distribution', fontsize=15, fontweight='bold', pad=20)
ax.grid(axis='y', alpha=0.4, linestyle='--')

# Add value labels on bars with better formatting
for bar in bars:
    height = bar.get_height()
    ax.text(bar.get_x() + bar.get_width()/2., height,
            f'${height:,.0f}',
            ha='center', va='bottom', fontweight='bold', fontsize=12)

plt.tight_layout()
plt.savefig(f'{output_dir}/03_asset_class_distribution.png', dpi=300, bbox_inches='tight', facecolor='white')
print(f"✓ Saved: {output_dir}/03_asset_class_distribution.png")
plt.close()

# ============================================================================
# Chart 4: Order Status Distribution (Pie Chart)
# ============================================================================
fig, ax = plt.subplots(figsize=(12, 8))
colors_orders = sns.color_palette("pastel", len(df_order_status))
wedges, texts, autotexts = ax.pie(
    df_order_status['count'],
    labels=df_order_status['status'],
    autopct='%1.1f%%',
    colors=colors_orders,
    startangle=45,
    textprops={'fontsize': 12, 'weight': 'bold'},
    explode=[0.1] * len(df_order_status),
    wedgeprops={'edgecolor': 'black', 'linewidth': 2}
)
ax.set_title('Order Status Distribution', fontsize=15, fontweight='bold', pad=20)

# Make percentage text more readable
for autotext in autotexts:
    autotext.set_color('black')
    autotext.set_fontsize(11)
    autotext.set_weight('bold')

plt.tight_layout()
plt.savefig(f'{output_dir}/04_order_status_distribution.png', dpi=300, bbox_inches='tight', facecolor='white')
print(f"✓ Saved: {output_dir}/04_order_status_distribution.png")
plt.close()

# ============================================================================
# Chart 5: Top 10 Holdings
# ============================================================================
fig, ax = plt.subplots(figsize=(14, 9))
df_top_holdings_sorted = df_top_holdings.sort_values('totalValue', ascending=True)
colors_holdings = sns.color_palette("rocket", len(df_top_holdings_sorted))
bars = ax.barh(df_top_holdings_sorted['ticker'], df_top_holdings_sorted['totalValue'], 
               color=colors_holdings, edgecolor='black', linewidth=1.5)
ax.set_xlabel('Total Value ($)', fontsize=13, fontweight='bold')
ax.set_ylabel('Ticker', fontsize=13, fontweight='bold')
ax.set_title('Top 10 Asset Holdings by Value', fontweight='bold', fontsize=15, pad=20)
ax.grid(axis='x', alpha=0.4, linestyle='--')

# Add value labels on bars
for i, bar in enumerate(bars):
    width = bar.get_width()
    ax.text(width, bar.get_y() + bar.get_height()/2., f' ${width:,.2f}',
            ha='left', va='center', fontweight='bold', fontsize=10)

plt.tight_layout()
plt.savefig(f'{output_dir}/05_top_holdings.png', dpi=300, bbox_inches='tight', facecolor='white')
print(f"✓ Saved: {output_dir}/05_top_holdings.png")
plt.close()

# ============================================================================
# Chart 6: User Access Levels
# ============================================================================
fig, ax = plt.subplots(figsize=(12, 8))
colors_access = sns.color_palette("Set1", len(df_access_level))
wedges, texts, autotexts = ax.pie(
    df_access_level['count'],
    labels=df_access_level['accessLevel'],
    autopct='%1.1f%%',
    colors=colors_access,
    startangle=90,
    textprops={'fontsize': 12, 'weight': 'bold'},
    explode=[0.1] * len(df_access_level),
    wedgeprops={'edgecolor': 'black', 'linewidth': 2}
)
ax.set_title('User Distribution by Access Level', fontweight='bold', fontsize=15, pad=20)

# Make percentage text more readable
for autotext in autotexts:
    autotext.set_color('white')
    autotext.set_fontsize(11)
    autotext.set_weight('bold')

plt.tight_layout()
plt.savefig(f'{output_dir}/06_access_levels.png', dpi=300, bbox_inches='tight', facecolor='white')
print(f"✓ Saved: {output_dir}/06_access_levels.png")
plt.close()

# ============================================================================
# Chart 7: Account Analysis - Dual Axis Chart (Improved)
# ============================================================================
fig, ax1 = plt.subplots(figsize=(14, 8))

# Bar chart for account count
x_pos = range(len(df_account_type))
bars = ax1.bar(x_pos, df_account_type['accountCount'], 0.5, label='Account Count', 
               alpha=0.8, color=sns.color_palette("Blues", 1)[0], edgecolor='black', linewidth=1.5)
ax1.set_ylabel('Number of Accounts', fontsize=13, fontweight='bold', color='darkblue')
ax1.set_xlabel('Account Type', fontsize=13, fontweight='bold')
ax1.set_title('Accounts and Balances by Type', fontweight='bold', fontsize=15, pad=20)
ax1.set_xticks(x_pos)
ax1.set_xticklabels(df_account_type['accountType'], rotation=45, ha='right')
ax1.tick_params(axis='y', labelcolor='darkblue')
ax1.grid(axis='y', alpha=0.4, linestyle='--')

# Add value labels on bars
for bar in bars:
    height = bar.get_height()
    ax1.text(bar.get_x() + bar.get_width()/2., height,
            f'{int(height)}',
            ha='center', va='bottom', fontweight='bold', fontsize=10, color='darkblue')

# Line chart for total balance (dual axis)
ax2 = ax1.twinx()
line = ax2.plot(x_pos, df_account_type['totalBalance'], 'o-', linewidth=3, markersize=10, 
                label='Total Balance', color='darkred', markerfacecolor='lightcoral', markeredgewidth=2, markeredgecolor='darkred')
ax2.set_ylabel('Total Balance ($)', fontsize=13, fontweight='bold', color='darkred')
ax2.tick_params(axis='y', labelcolor='darkred')

# Add value labels on line points
for i, (x, y) in enumerate(zip(x_pos, df_account_type['totalBalance'])):
    ax2.text(x, y + 800, f'${y:,.0f}', ha='center', va='bottom', fontweight='bold', fontsize=10, color='darkred')

# Add legend
lines1, labels1 = ax1.get_legend_handles_labels()
lines2, labels2 = ax2.get_legend_handles_labels()
ax1.legend(lines1 + lines2, labels1 + labels2, loc='upper left', fontsize=11, framealpha=0.95)

plt.tight_layout()
plt.savefig(f'{output_dir}/07_account_analysis.png', dpi=300, bbox_inches='tight', facecolor='white')
print(f"✓ Saved: {output_dir}/07_account_analysis.png")
plt.close()

# ============================================================================
# SUMMARY REPORT
# ============================================================================

print("\n" + "="*70)
print("ETL PIPELINE SUMMARY REPORT")
print("="*70)

print("\n📊 DATA EXTRACTION:")
print(f"  • Users extracted: {len(df_users)}")
print(f"  • Accounts created: {len(df_accounts)}")
print(f"  • Assets loaded: {len(df_assets)}")
print(f"  • Orders recorded: {len(df_orders)}")

print("\n💰 PORTFOLIO STATISTICS:")
total_portfolio_value = df_accounts['balance'].sum()
total_cash = df_accounts['cashBalance'].sum()
print(f"  • Total Portfolio Value: ${total_portfolio_value:,.2f}")
print(f"  • Total Cash on Hand: ${total_cash:,.2f}")
print(f"  • Number of Users: {len(df_users)}")
print(f"  • Average Balance per User: ${total_portfolio_value / len(df_users):,.2f}")

print("\n📈 TOP 5 USERS BY PORTFOLIO VALUE:")
for idx, row in df_user_totals.head(5).iterrows():
    print(f"  {idx+1}. {row['name']}: ${row['balance']:,.2f}")

print("\n🏦 ACCOUNT TYPE BREAKDOWN:")
for idx, row in df_account_type.iterrows():
    print(f"  • {row['accountType']}: {row['accountCount']} accounts, ${row['totalBalance']:,.2f}")

print("\n📊 ASSET CLASS DISTRIBUTION:")
for idx, row in df_asset_class.iterrows():
    pct = (row['totalValue'] / df_assets['totalValue'].sum()) * 100
    print(f"  • {row['assetClass']}: ${row['totalValue']:,.2f} ({pct:.1f}%)")

print("\n📋 ORDER STATUS BREAKDOWN:")
for idx, row in df_order_status.iterrows():
    pct = (row['count'] / len(df_orders)) * 100
    print(f"  • {row['status']}: {row['count']} orders ({pct:.1f}%)")

print("\n👥 USER ACCESS LEVELS:")
for idx, row in df_access_level.iterrows():
    pct = (row['count'] / len(df_users)) * 100
    print(f"  • {row['accessLevel']}: {row['count']} users ({pct:.1f}%)")

print("\n📊 CHARTS GENERATED:")
print(f"  ✓ 01_user_portfolio_values.png - Bar chart of user balances")
print(f"  ✓ 02_account_type_distribution.png - Pie chart by account type")
print(f"  ✓ 03_asset_class_distribution.png - Bar chart of asset classes")
print(f"  ✓ 04_order_status_distribution.png - Pie chart of order statuses")
print(f"  ✓ 05_top_holdings.png - Top 10 holdings by value")
print(f"  ✓ 06_access_levels.png - User access level distribution")
print(f"  ✓ 07_account_analysis.png - Account count vs balance analysis")
print(f"\n💾 Output directory: {output_dir}")
print("\n" + "="*70)
