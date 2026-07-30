package it.vasilepersonalsite.service;

import it.vasilepersonalsite.DTO.SkillDto;
import it.vasilepersonalsite.DTO.CategoryDto;
import it.vasilepersonalsite.DTO.KeywordDto;

import java.util.List;

public interface SkillService {

    // ===== SKILL =====
    List<SkillDto> findAllSkills();
    SkillDto findSkillById(Long id);
    SkillDto createSkill(SkillDto skillDto);
    SkillDto updateSkill(Long id, SkillDto updates);
    void deleteSkill(Long id);
    long countSkills();

    // ===== CATEGORY =====
    List<CategoryDto> findAllCategories();
    CategoryDto findCategoryById(Long id);
    CategoryDto createCategory(CategoryDto dto);
    CategoryDto updateCategory(Long id, CategoryDto dto);
    void deleteCategory(Long id);
    long countCategories();

    // ===== KEYWORD =====
    List<KeywordDto> findAllKeywords();
    KeywordDto findKeywordById(Long id);
    KeywordDto createKeyword(KeywordDto dto);
    KeywordDto updateKeyword(Long id, KeywordDto dto);
    void deleteKeyword(Long id);
    long countKeywords();
}
