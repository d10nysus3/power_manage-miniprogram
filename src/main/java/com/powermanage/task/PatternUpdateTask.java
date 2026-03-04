package com.powermanage.task;

import com.powermanage.entity.User;
import com.powermanage.repository.UserRepository;
import com.powermanage.service.ElectricityPatternAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PatternUpdateTask {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ElectricityPatternAnalysisService electricityPatternAnalysisService;

    /**
     * 每周日凌晨 00:00 批量更新所有活跃用户的画像
     * Cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 0 * * SUN")
    public void scheduledPatternUpdate() {
        System.out.println("开始执行每周画像批量更新任务: " + LocalDateTime.now());
        
        try {
            // 获取所有活跃用户
            // 假设 isActive 为 true 的用户
            List<User> activeUsers = userRepository.findAll(); 
            // 如果 UserRepository 没有 findByIsActiveTrue，则先取全部。
            // 实际项目中建议优化为分页获取或流式处理以避免 OOM，这里假设用户量尚可。
            
            int successCount = 0;
            int failCount = 0;
            
            for (User user : activeUsers) {
                // 简单的活跃判断，如果有 isActive 字段最好
                if (Boolean.TRUE.equals(user.getIsActive())) {
                    try {
                        System.out.println("正在更新用户 " + user.getId() + " (" + user.getName() + ") 的画像...");
                        electricityPatternAnalysisService.generateAndSaveUserPattern(user.getId());
                        successCount++;
                    } catch (Exception e) {
                        System.err.println("用户 " + user.getId() + " 画像更新失败: " + e.getMessage());
                        failCount++;
                    }
                }
            }
            
            System.out.println("画像批量更新任务完成。成功: " + successCount + ", 失败: " + failCount);
            
        } catch (Exception e) {
            System.err.println("画像批量更新任务发生严重错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
