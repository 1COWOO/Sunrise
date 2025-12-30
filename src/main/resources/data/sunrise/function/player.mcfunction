# 1. 폭죽 소환
function sunrise:summon

# 2. 왼손 아이템을 메인핸드(오른손)로 강제 이동
# (왼손을 비워버림으로써 연사 조건 자체를 파괴)
item replace entity @s weapon.mainhand from entity @s weapon.offhand
item replace entity @s weapon.offhand with air
clear @s minecraft:firework_rocket 1
