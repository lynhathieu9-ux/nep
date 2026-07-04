<template>
  <header class="dashboard-header">
    <div class="header-bg"></div>
    <div class="header-glow-line"></div>
  </header>
</template>

<script setup>
</script>

<style scoped>
.dashboard-header {
  position: relative;
  height: 56px;
  background: linear-gradient(180deg, rgba(8, 18, 35, 0.98) 0%, rgba(5, 12, 22, 0.95) 100%);
  border-bottom: 1px solid rgba(64, 144, 188, 0.2);
  overflow: hidden;
  z-index: 100;
}

.header-bg {
  position: absolute;
  inset: 0;
  background: 
    radial-gradient(ellipse at 20% 50%, rgba(64, 144, 188, 0.1) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 50%, rgba(168, 85, 247, 0.08) 0%, transparent 50%);
}

.header-glow-line {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, 
    transparent 0%, 
    rgba(64, 144, 188, 0.3) 15%, 
    rgba(64, 144, 188, 0.6) 50%, 
    rgba(64, 144, 188, 0.3) 85%, 
    transparent 100%);
  border-radius: 2px;
}
</style>