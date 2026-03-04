import * as ElIcons from '@element-plus/icons-vue';


const chartIcons = Object.keys(ElIcons).filter(icon => 
  icon.toLowerCase().includes('chart')
);

console.log('图表相关的图标：');
console.log(chartIcons);


const barChartIcons = Object.keys(ElIcons).filter(icon => 
  icon.toLowerCase().includes('bar')
);

console.log('\n柱状图相关的图标：');
console.log(barChartIcons);