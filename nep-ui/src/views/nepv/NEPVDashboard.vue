<template>
  <div class="dashboard-wrapper">
    <div class="dashboard-bg">
      <div class="bg-gradient"></div>
      <div class="bg-grid"></div>
      <div class="bg-glow"></div>
    </div>

    <Header />
    <TopCards />

    <div class="main-content">
      <div class="map-area">
        <Map3D />
        <RightPanel />
        <LeftPanel />
      </div>

      <BottomCharts />
    </div>
  </div>
</template>

<script setup>
import Header from '@/components/dashboard/Header.vue'
import TopCards from '@/components/dashboard/TopCards.vue'
import Map3D from '@/components/dashboard/Map3D.vue'
import RightPanel from '@/components/dashboard/RightPanel.vue'
import LeftPanel from '@/components/dashboard/LeftPanel.vue'
import BottomCharts from '@/components/dashboard/BottomCharts.vue'
</script>

<style scoped>
.dashboard-wrapper {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dashboard-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
}

.bg-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, #050d18 0%, #0a1628 30%, #0f1e36 60%, #0a1628 100%);
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(64, 144, 188, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(64, 144, 188, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
}

.bg-glow {
  position: absolute;
  inset: 0;
  background: 
    radial-gradient(ellipse at 30% 20%, rgba(64, 144, 188, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 70% 80%, rgba(168, 85, 247, 0.05) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(42, 168, 118, 0.03) 0%, transparent 60%);
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.map-area {
  flex: 1;
  position: relative;
  min-height: 400px;
}

@media(max-width: 1400px) {
  .map-area {
    min-height: 350px;
  }
}

@media(max-width: 1024px) {
  .map-area {
    min-height: 300px;
  }
}

@media(max-width: 768px) {
  .map-area {
    min-height: 250px;
  }
}
</style>