<template>
  <div class="bc">
    <!-- 上排：3列图表 -->
    <div class="bc-row">
      <div class="bc-card">
        <h5><i></i>污染热点分布 <span>// HOTSPOT DISTRIBUTION</span></h5>
        <div class="bc-pie-w">
          <div ref="ringChartRef" class="bc-ch"></div>
          <div class="bc-cv">2307</div>
        </div>
      </div>
      <div class="bc-card">
        <h5><i></i>城市AQI柱群 <span>// CITY AQI BARS</span></h5>
        <div class="bc-bar-w">
          <span class="bc-fv">2307</span>
          <div ref="cityBarRef" class="bc-ch"></div>
        </div>
      </div>
      <div class="bc-card">
        <h5><i></i>反馈状态分布 <span>// STATUS DISTRIBUTION</span></h5>
        <div class="bc-pie-w">
          <div ref="pieChartRef" class="bc-ch"></div>
          <div class="bc-cv sm">2307</div>
        </div>
      </div>
    </div>

    <!-- 下排：3列图表 -->
    <div class="bc-row">
      <div class="bc-card">
        <h5><i></i>月度反馈统计 <span>// MONTHLY TREND</span></h5>
        <div ref="lineChartRef" class="bc-ch"></div>
      </div>
      <div class="bc-card">
        <h5><i></i>各省份反馈统计 <span>// PROVINCE STATS</span></h5>
        <div ref="provinceChartRef" class="bc-ch"></div>
      </div>
      <div class="bc-card">
        <h5><i></i>污染物超标累计 <span>// POLLUTANT EXCEEDANCE</span></h5>
        <div ref="pollutantChartRef" class="bc-ch"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, markRaw } from 'vue'
import * as echarts from 'echarts'
import { getFeedbackStatus, getProvinceFeedback, getMonthlyTrend, getProvincePollutantExceed } from '@/api/statistics'

const ringChartRef=ref(null),cityBarRef=ref(null),pieChartRef=ref(null),lineChartRef=ref(null),provinceChartRef=ref(null),pollutantChartRef=ref(null)
let charts=[]

// 统一配色系：主色青蓝 + 点缀霓虹粉/金
var C={cyan:'#00E5FF',blue:'#409EFF',green:'#10AC84',orange:'#F59E0B',red:'#FF3366',purple:'#9B59B6',text:'#8A9EBC',sub:'#546A85',grid:'rgba(255,255,255,0.04)',bg:'rgba(8,18,36,0.92)'}
function gd(c1,c2){return new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:c1},{offset:1,color:c2}])}

// ====== 仪表盘环图 ======
function initRing(){
  if(!ringChartRef.value)return
  var c=markRaw(echarts.init(ringChartRef.value))
  c.setOption({
    series:[{type:'gauge',startAngle:90,endAngle:-270,pointer:{show:false},
      progress:{show:true,overlap:false,roundCap:true,clip:false,width:10,
        itemStyle:{color:new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:C.cyan},{offset:0.4,color:C.purple},{offset:1,color:C.red}])}
      },
      axisLine:{lineStyle:{width:10,color:[[1,'rgba(255,255,255,0.04)']]}},
      splitLine:{show:false},axisTick:{show:false},axisLabel:{show:false},
      detail:{show:false},
      data:[{value:75}]
    }]
  });charts.push(c)
}

// ====== 城市柱群：2D渐变柱 + 背景暗柱 ======
function initCityBars(){
  if(!cityBarRef.value)return
  var c=markRaw(echarts.init(cityBarRef.value))
  var cities=['北京','天津','河北','山西','内蒙古','辽宁','吉林','黑龙江','上海','江苏']
  var vals=[160,145,130,120,115,110,105,100,95,90]
  c.setOption({
    tooltip:{trigger:'axis',backgroundColor:C.bg,borderColor:'rgba(0,229,255,0.2)',textStyle:{color:'#fff',fontSize:11}},
    grid:{left:'5%',right:'5%',bottom:'10%',top:'8%',containLabel:true},
    xAxis:{type:'category',data:cities,axisLabel:{color:C.text,fontSize:9},axisLine:{lineStyle:{color:C.sub}},axisTick:{show:false}},
    yAxis:{show:false},
    // 背景暗柱系列
    series:[{type:'bar',barWidth:'55%',barGap:'-100%',data:vals.map(function(){return 200}),
      itemStyle:{color:'rgba(255,255,255,0.03)',borderRadius:[6,6,0,0]},z:0
    },{type:'bar',barWidth:'55%',
      data:vals.map(function(v,i){var colors=[gd(C.cyan,'rgba(0,229,255,0.1)'),gd(C.purple,'rgba(155,89,182,0.1)'),gd(C.green,'rgba(16,172,132,0.1)')];return{value:v,itemStyle:{color:colors[i%3],borderRadius:[6,6,0,0]}}}),z:2
    }]
  });charts.push(c)
}

// ====== 反馈饼图：间隙+圆角+轨道 ======
function initPie(){
  if(!pieChartRef.value)return
  var c=markRaw(echarts.init(pieChartRef.value))
  c.setOption({
    series:[{type:'pie',radius:['52%','70%'],center:['50%','50%'],
      label:{show:false},labelLine:{show:false},
      itemStyle:{borderColor:'#030A18',borderWidth:4,borderRadius:3},
      emphasis:{scaleSize:6},
      data:[
        {value:30,name:'待处理',itemStyle:{color:gd(C.orange,C.red)}},
        {value:40,name:'处理中',itemStyle:{color:gd(C.cyan,C.blue)}},
        {value:30,name:'已完成',itemStyle:{color:gd(C.green,'rgba(16,172,132,0.3)')}}
      ]
    }]
  });charts.push(c)
}

// ====== 月度趋势：渐变柱+圆角+弱化网格 ======
function initLine(){
  if(!lineChartRef.value)return
  var c=markRaw(echarts.init(lineChartRef.value))
  var months=['1月','2月','3月','4月','5月','6月'],vals=[120,180,150,220,190,250]
  c.setOption({
    animationDuration:1000,animationEasing:'cubicOut',
    tooltip:{trigger:'axis',backgroundColor:C.bg,borderColor:'rgba(0,229,255,0.2)',textStyle:{color:'#fff',fontSize:11}},
    grid:{left:'5%',right:'5%',bottom:'12%',top:'10%',containLabel:true},
    xAxis:{type:'category',data:months,axisLabel:{color:C.text,fontSize:9},axisLine:{lineStyle:{color:C.sub}},axisTick:{show:false}},
    yAxis:{type:'value',axisLabel:{color:C.text,fontSize:9},splitLine:{lineStyle:{color:C.grid,type:'dashed'}}},
    series:[{type:'bar',barWidth:'45%',
      data:vals.map(function(v){return{value:v,itemStyle:{color:gd(C.cyan,'rgba(0,229,255,0.05)'),borderRadius:[5,5,0,0]}}})
    }]
  });charts.push(c)
}

// ====== 省份统计 ======
function initProvince(){
  if(!provinceChartRef.value)return
  var c=markRaw(echarts.init(provinceChartRef.value))
  var provinces=['北京','天津','河北','山西','内蒙古','辽宁','吉林','黑龙江']
  c.setOption({
    animationDuration:1000,animationEasing:'cubicOut',
    tooltip:{trigger:'axis',backgroundColor:C.bg,borderColor:'rgba(0,229,255,0.2)',textStyle:{color:'#fff',fontSize:11}},
    grid:{left:'5%',right:'5%',bottom:'14%',top:'10%',containLabel:true},
    xAxis:{type:'category',data:provinces,axisLabel:{color:C.text,fontSize:8,rotate:30},axisLine:{lineStyle:{color:C.sub}},axisTick:{show:false}},
    yAxis:{type:'value',axisLabel:{color:C.text,fontSize:9},splitLine:{lineStyle:{color:C.grid,type:'dashed'}}},
    series:[{type:'bar',barWidth:'50%',
      data:[85,65,120,75,45,90,55,60].map(function(v){return{value:v,itemStyle:{color:gd(C.purple,'rgba(155,89,182,0.05)'),borderRadius:[5,5,0,0]}}})
    }]
  });charts.push(c)
}

// ====== 污染物堆叠柱 ======
function initPollutant(){
  if(!pollutantChartRef.value)return
  var c=markRaw(echarts.init(pollutantChartRef.value))
  var provinces=['北京','天津','河北','山西','内蒙古','辽宁','吉林','黑龙江']
  c.setOption({
    animationDuration:1000,animationEasing:'cubicOut',
    tooltip:{trigger:'axis',axisPointer:{type:'shadow'},backgroundColor:C.bg,borderColor:'rgba(0,229,255,0.2)',textStyle:{color:'#fff',fontSize:11}},
    legend:{data:['SO₂','CO','PM2.5'],textStyle:{color:C.text,fontSize:9},top:0,right:0,icon:'rect',itemWidth:7,itemHeight:7},
    grid:{left:'5%',right:'5%',bottom:'14%',top:'16%',containLabel:true},
    xAxis:{type:'category',data:provinces,axisLabel:{color:C.text,fontSize:8,rotate:30},axisLine:{lineStyle:{color:C.sub}},axisTick:{show:false}},
    yAxis:{type:'value',axisLabel:{color:C.text,fontSize:9},splitLine:{lineStyle:{color:C.grid,type:'dashed'}}},
    series:[
      {name:'SO₂',type:'bar',stack:'t',barWidth:'50%',data:[25,20,35,28,15,22,18,16],itemStyle:{color:gd(C.orange,'rgba(245,158,11,0.05)'),borderRadius:[0,0,0,0]}},
      {name:'CO',type:'bar',stack:'t',data:[18,15,25,20,12,18,14,12],itemStyle:{color:gd(C.red,'rgba(255,51,102,0.05)')}},
      {name:'PM2.5',type:'bar',stack:'t',data:[32,28,42,35,20,30,22,20],itemStyle:{color:gd(C.purple,'rgba(155,89,182,0.05)')}}
    ]
  });charts.push(c)
}

// ====== 用真实数据初始化图表 ======
function initRingWithData(completionRate){ if(!ringChartRef.value)return;var c=markRaw(echarts.init(ringChartRef.value));c.setOption({series:[{data:[{value:Math.min(completionRate,100)}]}]});charts.push(c)}
function initCityBarsWithData(data){ if(!cityBarRef.value)return;var c=markRaw(echarts.init(cityBarRef.value));var top8=data.slice(0,8);var maxVal=Math.max.apply(null,top8.map(function(d){return d.count||0}).concat([100]));c.setOption({xAxis:{data:top8.map(function(d){return d.provinceName})},series:[{data:top8.map(function(){return maxVal})},{data:top8.map(function(d,i){var colors=[gd(C.cyan,'rgba(0,229,255,0.1)'),gd(C.purple,'rgba(155,89,182,0.1)'),gd(C.green,'rgba(16,172,132,0.1)')];return{value:d.count||0,itemStyle:{color:colors[i%3],borderRadius:[6,6,0,0]}}})}]});charts.push(c)}
function initPieWithData(data){ if(!pieChartRef.value)return;var c=markRaw(echarts.init(pieChartRef.value));c.setOption({series:[{data:[{value:data.pending||0,name:'待处理',itemStyle:{color:gd(C.orange,C.red)}},{value:data.assigned||0,name:'处理中',itemStyle:{color:gd(C.cyan,C.blue)}},{value:data.completed||0,name:'已完成',itemStyle:{color:gd(C.green,'rgba(16,172,132,0.3)')}}]}]});charts.push(c)}
function initLineWithData(data){
  if(!lineChartRef.value)return
  var c=markRaw(echarts.init(lineChartRef.value))
  var months=data.map(function(d){return d.month})
  var vals=data.map(function(d){return d.count})
  var colorPairs=[['#00E5A0','#00B87A'],['#00E5FF','#0099CC'],['#409EFF','#2563EB'],['#C084FC','#7C3AED'],['#F472B6','#DB2777'],['#FB923C','#EA580C']]
  var barData=vals.map(function(v,i){return{value:v,itemStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:colorPairs[i][0]},{offset:1,color:colorPairs[i][1]}]),borderRadius:[6,6,0,0]}}})
  c.setOption({
    animationDuration:1200,animationEasing:'cubicOut',
    tooltip:{trigger:'axis',backgroundColor:'rgba(10,25,49,0.94)',borderColor:'rgba(0,229,255,0.25)',borderWidth:1,padding:[10,14],textStyle:{color:'#fff',fontSize:12},extraCssText:'backdrop-filter:blur(12px);border-radius:8px;'},
    grid:{left:'5%',right:'5%',bottom:'10%',top:'12%',containLabel:true},
    xAxis:{type:'category',data:months,axisLabel:{color:C.text,fontSize:10},axisLine:{lineStyle:{color:'rgba(255,255,255,0.08)'}},axisTick:{show:false}},
    yAxis:{type:'value',axisLabel:{color:C.text,fontSize:9},splitLine:{lineStyle:{color:'rgba(255,255,255,0.04)',type:'dashed'}}},
    series:[{type:'bar',barWidth:'45%',data:barData,
      emphasis:{itemStyle:{shadowBlur:12,shadowColor:function(p){return(colorPairs[p.dataIndex]||['#00E5FF'])[0]+'60'}}}
    }]
  });charts.push(c)}
function initProvinceWithData(data){ if(!provinceChartRef.value)return;var c=markRaw(echarts.init(provinceChartRef.value));c.setOption({xAxis:{data:data.slice(0,8).map(function(d){return d.provinceName})},series:[{data:data.slice(0,8).map(function(d){return{value:d.count,itemStyle:{color:gd(C.purple,'rgba(155,89,182,0.05)'),borderRadius:[5,5,0,0]}}})}]});charts.push(c)}
function initPollutantWithData(data){ if(!pollutantChartRef.value)return;var c=markRaw(echarts.init(pollutantChartRef.value));var pd=data.filter(function(d){return d.provinceName}).slice(0,8);c.setOption({xAxis:{data:pd.map(function(d){return d.provinceName})},series:[{data:pd.map(function(d){return Number(d.so2Exceed)||0})},{data:pd.map(function(d){return Number(d.coExceed)||0})},{data:pd.map(function(d){return Number(d.pm25Exceed)||0})}]});charts.push(c)}

// 更新中心数值
function updateCenterVals(total){var els=document.querySelectorAll('.bc-cv');if(els.length>=2){els[0].textContent=total;els[1].textContent=total}}

onMounted(async function(){
  try{
    var[fs,pf,mt,pe]=await Promise.all([getFeedbackStatus(),getProvinceFeedback(),getMonthlyTrend(),getProvincePollutantExceed()])
    var fdata=fs&&fs.data?fs.data:{pending:0,assigned:0,completed:0}
    var total=fdata.pending+fdata.assigned+fdata.completed
    var completionRate=total>0?Math.round(fdata.completed/total*100):0
    var pdata=pf&&pf.data?pf.data:[]
    var mdata=mt&&mt.data?mt.data:[]
    var edata=pe&&pe.data?pe.data:[]
    var cityTotal=pdata.reduce(function(s,d){return s+(d.count||0)},0)

    initRingWithData(completionRate)
    initCityBarsWithData(pdata)
    initPieWithData(fdata)
    initLineWithData(mdata)
    initProvinceWithData(pdata)
    initPollutantWithData(edata)
    updateCenterVals(cityTotal)
  }catch(e){
    console.error('图表数据加载失败:',e)
    // 降级：用默认数据初始化
    initRing();initCityBars();initPie();initLine();initProvince();initPollutant()
  }

  window.addEventListener('resize',function(){charts.forEach(function(c){c.resize()})})
})

onUnmounted(function(){window.removeEventListener('resize',function(){});charts.forEach(function(c){c.dispose()});charts=[]})
</script>

<style scoped>
/* 整体 */
.bc{display:flex;flex-direction:column;gap:14px;padding:14px 16px 16px;}
.bc-row{display:grid;grid-template-columns:repeat(3,1fr);gap:14px;}

/* 卡片：毛玻璃+微光边框+角标 */
.bc-card{
  position:relative;
  background:rgba(10,25,49,0.55);backdrop-filter:blur(14px);-webkit-backdrop-filter:blur(14px);
  border:1px solid rgba(0,229,255,0.08);border-radius:10px;padding:16px 18px;
  box-shadow:inset 0 1px 0 rgba(0,229,255,0.06),0 4px 20px rgba(0,0,0,0.4);
  transition:transform 0.25s,border-color 0.25s,box-shadow 0.25s;
  overflow:hidden;
}
.bc-card::before{content:'';position:absolute;top:0;left:0;right:0;height:1px;background:linear-gradient(90deg,transparent,rgba(0,229,255,0.3),transparent);}
.bc-card::after{content:'';position:absolute;top:6px;left:6px;width:8px;height:8px;border-top:1px solid rgba(0,229,255,0.3);border-left:1px solid rgba(0,229,255,0.3);}
.bc-card:hover{border-color:rgba(0,229,255,0.18);transform:translateY(-2px);box-shadow:inset 0 1px 0 rgba(0,229,255,0.1),0 8px 28px rgba(0,0,0,0.5),0 0 20px rgba(0,229,255,0.06);}

/* 标题：左发光竖线+装饰英文 */
.bc-card h5{font-size:13px;font-weight:600;color:#E0E8F5;margin:0 0 14px;display:flex;align-items:center;gap:8px;}
.bc-card h5 i{display:inline-block;width:2px;height:14px;background:#00E5FF;border-radius:1px;box-shadow:0 0 6px rgba(0,229,255,0.5);}
.bc-card h5 span{font-size:9px;font-weight:400;color:rgba(138,158,188,0.4);letter-spacing:1px;margin-left:2px;}

/* 图表 */
.bc-ch{width:100%;height:210px;}
.bc-pie-w{position:relative;}
.bc-cv{position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);font-size:26px;font-weight:700;color:#fff;font-family:'DIN Alternate','SF Mono',monospace;text-shadow:0 0 16px rgba(0,229,255,0.5);pointer-events:none;letter-spacing:2px;}
.bc-cv.sm{font-size:20px;}
.bc-bar-w{position:relative;}
.bc-fv{position:absolute;top:4px;left:10px;font-size:16px;font-weight:700;color:#F59E0B;font-family:'DIN Alternate',monospace;text-shadow:0 0 8px rgba(245,158,11,0.4);z-index:5;}

@media(max-width:1400px){.bc-row{grid-template-columns:repeat(2,1fr)}}
@media(max-width:900px){.bc-row{grid-template-columns:1fr}}
</style>
