execute as @a run attribute @s minecraft:attack_speed base set 99999999999

# [1단계] 로켓 비행 중
execute as @e[type=minecraft:firework_rocket,tag=newyear] at @s run fill ~-1 ~-1 ~-1 ~1 ~1 ~1 air replace minecraft:light
execute as @e[type=minecraft:firework_rocket,tag=newyear] at @s run setblock ~ ~ ~ minecraft:light[level=8] replace

# [2단계] 폭죽이 터지는 순간 (Life:20)
execute as @e[type=minecraft:firework_rocket,nbt={Life:20},tag=newyear] at @s run summon minecraft:marker ~ ~ ~ {Tags:["sunrise_manager","temp","light_active"]}

# 세팅 변경: 파티클 타이머 10(0.5초), 빛 타이머 20(1초)로 단축
execute as @e[tag=temp] run scoreboard players set @s timer 10
execute as @e[tag=temp] run scoreboard players set @s light_timer 20
execute as @e[tag=temp] run tag @s remove temp

# 터지는 순간 처리
execute as @e[type=minecraft:firework_rocket,nbt={Life:20},tag=newyear] at @s run setblock ~ ~ ~ minecraft:light[level=15] replace
execute as @e[type=minecraft:firework_rocket,nbt={Life:20},tag=newyear] at @s run particle flash{color:[0.000,0.627,0.965,1.00]} ~ ~ ~ 0 0 0 0 1
execute as @e[type=minecraft:firework_rocket,nbt={Life:20},tag=newyear] at @s run function sunrise:document
execute as @e[type=minecraft:firework_rocket,nbt={Life:20},tag=newyear] at @s run playsound minecraft:entity.firework_rocket.large_blast master @a ~ ~ ~ 1 1
execute as @e[type=minecraft:firework_rocket,nbt={Life:20},tag=newyear] at @s run kill @s

# [3단계] 타이머 감소
execute as @e[tag=sunrise_manager] run scoreboard players remove @s timer 1
execute as @e[tag=sunrise_manager,tag=light_active] run scoreboard players remove @s light_timer 1

# [4단계] 파티클 유지 (10틱 동안만 실행 - 렉 대폭 감소)
execute as @e[tag=sunrise_manager,scores={timer=1..}] at @s run function sunrise:document

# [5단계] 빛 블록 제거 (20틱 = 1초 뒤 삭제)
execute as @e[tag=light_active,scores={light_timer=..0}] at @s run fill ~-2 ~-2 ~-2 ~2 ~2 ~2 air replace minecraft:light
execute as @e[tag=light_active,scores={light_timer=..0}] run tag @s remove light_active

# [6단계] 마커 최종 삭제
execute as @e[tag=sunrise_manager,scores={timer=..0},tag=!light_active] run kill @s

# [7단계] 왼손 감지 (성능을 위해 태그가 없는 경우에만 실행하는 것을 권장하나, 기존 로직 유지)
execute as @a if items entity @s weapon.offhand minecraft:firework_rocket run function sunrise:player
