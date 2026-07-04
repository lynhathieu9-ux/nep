<template>
  <div class="mc">
    <div class="mc-bg"><div class="bg1"></div><div class="bg2"></div><div class="bg3"></div></div>
    <div ref="chartRef" class="mc-chart"></div>

    <!-- 返回按钮 -->
    <div v-if="level==='city'" class="mc-back" @click="backToChina">← 返回全国 · {{ curProv }}</div>

    <!-- 图例 -->
    <div class="mc-leg">
      <span class="mc-lt">AQI 污染等级</span>
      <div class="mc-li" v-for="l in levels" :key="l.label"><em :style="{background:l.color}"></em>{{l.label}} {{l.range}}</div>
    </div>

    <div class="mc-parts">
      <div v-for="p in particles" :key="p.id" class="mc-p" :style="p.style"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getMapAqi } from '@/api/statistics'

const chartRef = ref(null)
const level = ref('china')
const curProv = ref('')
const levels = [
  {color:'#059669',label:'优',range:'0-50'},{color:'#D97706',label:'良',range:'51-100'},
  {color:'#F59E0B',label:'轻度',range:'101-150'},{color:'#E11D48',label:'中度',range:'151-200'},
  {color:'#9F1239',label:'重度',range:'201-300'},{color:'#4C0519',label:'严重',range:'>300'}
]
const particles = []
for(var i=0;i<40;i++) particles.push({id:i,style:{left:Math.random()*100+'%',top:Math.random()*100+'%',animationDelay:Math.random()*5+'s',animationDuration:(3+Math.random()*4)+'s',opacity:Math.random()*0.5+0.3}})

let chart=null, cityData=[], provData=[], geoCache={}
var CHINA_URL='https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json'

var PROV_MAP={ '北京':110000,'北京市':110000,'天津':120000,'天津市':120000,'河北省':130000,'河北':130000,'山西省':140000,'山西':140000,'内蒙古自治区':150000,'内蒙古':150000,'辽宁省':210000,'辽宁':210000,'吉林省':220000,'吉林':220000,'黑龙江省':230000,'黑龙江':230000,'上海市':310000,'上海':310000,'江苏省':320000,'江苏':320000,'浙江省':330000,'浙江':330000,'安徽省':340000,'安徽':340000,'福建省':350000,'福建':350000,'江西省':360000,'江西':360000,'山东省':370000,'山东':370000,'河南省':410000,'河南':410000,'湖北省':420000,'湖北':420000,'湖南省':430000,'湖南':430000,'广东省':440000,'广东':440000,'广西壮族自治区':450000,'广西':450000,'海南省':460000,'海南':460000,'重庆市':500000,'重庆':500000,'四川省':510000,'四川':510000,'贵州省':520000,'贵州':520000,'云南省':530000,'云南':530000,'西藏自治区':540000,'西藏':540000,'陕西省':610000,'陕西':610000,'甘肃省':620000,'甘肃':620000,'青海省':630000,'青海':630000,'宁夏回族自治区':640000,'宁夏':640000,'新疆维吾尔自治区':650000,'新疆':650000,'台湾省':710000,'台湾':710000,'香港特别行政区':810000,'香港':810000,'澳门特别行政区':820000,'澳门':820000 }

function getAqiColor(v){ if(v==null||v<0)return'#1e293b';for(var i=0;i<levels.length;i++){if(v<=levels[i].max)return levels[i].color}return'#4C0519' }
function findCode(n){ return PROV_MAP[n]||null }

async function loadGeo(url){ if(geoCache[url])return geoCache[url];var r=await fetch(url);var j=await r.json();geoCache[url]=j;return j }

function matchPN(n){ var s=n.replace(/省|市|自治区|维吾尔自治区|壮族自治区|回族自治区|特别行政区/g,'');return[n,s] }

async function renderChina(){
  if(!chart)return
  var geo=await loadGeo(CHINA_URL); echarts.registerMap('china',geo)
  var pm={}
  provData.forEach(function(p){if(p.provinceName){var ns=matchPN(p.provinceName);ns.forEach(function(n){pm[n]={aqi:p.avgAqi,count:p.totalDetections||0}})}})
  chart.off('click')
  chart.on('click',async function(params){
    if(params.componentType==='series'&&level.value==='china'){var code=findCode(params.name);if(code)await drillDown(params.name,code)}
  })
  chart.setOption({
    backgroundColor:'transparent',animationDuration:600,
    geo:{map:'china',roam:true,zoom:1.2,center:[105,36],scaleLimit:{min:1,max:8},label:{show:false},itemStyle:{areaColor:'#162a4a',borderColor:'rgba(74,144,226,0.4)',borderWidth:1},emphasis:{label:{show:true,fontSize:13,color:'#fff'},itemStyle:{areaColor:'rgba(74,144,226,0.35)',borderColor:'rgba(74,144,226,0.8)',borderWidth:2}}},
    series:[{type:'map',map:'china',geoIndex:0,label:{show:false},
      data:Object.keys(pm).map(function(n){return{name:n,value:pm[n].aqi}}),
      itemStyle:{borderColor:'rgba(74,144,226,0.25)',borderWidth:0.6}
    },{type:'effectScatter',coordinateSystem:'geo',
      data:provData.filter(function(p){return p.avgAqi!=null}).map(function(p){var c=[(p.longitude||116),(p.latitude||39),p.avgAqi];return{name:p.provinceName,value:c,aqi:p.avgAqi}}),
      symbolSize:function(v){return Math.max(3,Math.min((v[2]||50)/6,10))},
      showEffectOn:'render',
      rippleEffect:{brushType:'stroke',scale:2.5,period:3},
      itemStyle:{color:function(p){return getAqiColor(p.data.aqi)},opacity:0.9},symbol:'diamond',zlevel:5
    },{type:'effectScatter',coordinateSystem:'geo',
      data:provData.filter(function(p){return p.avgAqi!=null&&p.avgAqi>80}).slice(0,10).map(function(p){var c=[(p.longitude||116),(p.latitude||39),p.avgAqi];return{name:p.provinceName,value:c,aqi:p.avgAqi}}),
      symbolSize:function(v){return Math.max(5,Math.min((v[2]||50)/3,16))},
      showEffectOn:'render',
      rippleEffect:{brushType:'stroke',scale:3,period:2},
      itemStyle:{color:'#fff',opacity:0.9},symbol:'star',zlevel:6
    }]
  },true)
  level.value='china';curProv.value=''
}

async function drillDown(name,code){
  if(!chart)return; curProv.value=name
  var url='https://geo.datav.aliyun.com/areas_v3/bound/'+code+'_full.json'
  var geo=await loadGeo(url); var mn='prov_'+code; echarts.registerMap(mn,geo)
  var cm={}
  cityData.forEach(function(c){if(c.provinceName&&matchPN(c.provinceName).some(function(n){return n===name||name.includes(n)||n.includes(name)})){var sn=c.cityName.replace(/市|州|地区|盟|自治州/g,'');cm[sn]={aqi:c.avgAqi,count:c.detectionCount};cm[c.cityName]={aqi:c.avgAqi,count:c.detectionCount}}})
  chart.off('click')
  chart.setOption({
    backgroundColor:'transparent',animationDuration:500,
    geo:{map:mn,roam:true,zoom:1.2,scaleLimit:{min:1,max:8},label:{show:false},itemStyle:{areaColor:'#162a4a',borderColor:'rgba(74,144,226,0.35)',borderWidth:1},emphasis:{label:{show:true,fontSize:12,color:'#fff'},itemStyle:{areaColor:'rgba(74,144,226,0.3)',borderColor:'rgba(74,144,226,0.7)'}}},
    series:[{type:'map',map:mn,geoIndex:0,label:{show:true,fontSize:10,color:'#8A9EBC'},
      data:geo.features.map(function(f){var gn=f.properties.name;var info=cm[gn];return{name:gn,value:info?info.aqi:-1}}),
      itemStyle:{borderColor:'rgba(74,144,226,0.2)',borderWidth:0.6}
    }]
  },false)
  level.value='city'
}

async function backToChina(){ await renderChina() }

onMounted(async function(){
  try{var r=await getMapAqi();cityData=r.data.cities||[];provData=r.data.provinces||[]}catch(e){}
  if(chartRef.value){chart=echarts.init(chartRef.value);await renderChina();window.addEventListener('resize',function(){chart&&chart.resize()})}
})
onUnmounted(function(){window.removeEventListener('resize',function(){});chart&&chart.dispose();chart=null})
</script>

<style scoped>
.mc{position:relative;width:100%;height:100%;overflow:hidden;}
.mc-bg{position:absolute;inset:0;z-index:0}
.bg1{position:absolute;inset:0;background:linear-gradient(135deg,rgba(8,22,45,0.98),rgba(12,30,60,0.96),rgba(18,42,78,0.94),rgba(8,22,45,0.98))}
.bg2{position:absolute;inset:0;background:radial-gradient(ellipse at 50% 50%,rgba(74,144,226,0.06),transparent 60%)}
.bg3{position:absolute;inset:0;opacity:0.025;background-image:url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E")}
.mc-chart{position:relative;width:100%;height:100%;z-index:10}

.mc-back{position:absolute;top:14px;left:16px;z-index:30;padding:8px 18px;background:rgba(8,22,45,0.9);border:1px solid rgba(255,255,255,0.1);border-radius:20px;color:#c8d6e5;font-size:12px;font-weight:600;cursor:pointer;transition:all 0.25s}
.mc-back:hover{color:#fff;border-color:rgba(255,255,255,0.25)}

.mc-leg{position:absolute;bottom:22px;left:50%;transform:translateX(-50%);z-index:25;display:flex;align-items:center;gap:14px;background:rgba(10,22,42,0.92);backdrop-filter:blur(12px);border-radius:10px;padding:10px 20px;border:1px solid rgba(74,144,226,0.2);box-shadow:0 4px 20px rgba(0,0,0,0.4)}
.mc-lt{font-size:11px;font-weight:600;color:#94a3b8;white-space:nowrap}
.mc-li{font-size:10px;color:#cbd5e1;display:flex;align-items:center;gap:5px}
.mc-li em{display:inline-block;width:10px;height:10px;border-radius:2px}

.mc-parts{position:absolute;inset:0;pointer-events:none;z-index:5;overflow:hidden}
.mc-p{position:absolute;width:2px;height:2px;background:rgba(255,255,255,0.7);border-radius:50%;box-shadow:0 0 8px rgba(255,255,255,0.6);animation:tw 3s infinite ease-in-out}
@keyframes tw{0%,100%{opacity:0.2;transform:scale(0.5)}50%{opacity:1;transform:scale(1.2)}}
</style>
