import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    Image,
    KeyboardAvoidingView, Platform,
    SafeAreaView,
    StyleSheet,
    Text, TextInput, TouchableOpacity,
    View
} from 'react-native';
import { fetchApi } from '../../lib/api';

export default function MyPageEditScreen() {
  const router = useRouter();
  const [nickname, setNickname] = useState("");
  const [originalNickname, setOriginalNickname] = useState("");
  const [profileImgUri, setProfileImgUri] = useState<string | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string>("https://via.placeholder.com/150?text=Profile");
  
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [isChecked, setIsChecked] = useState(true);

  useEffect(() => {
    fetchApi<any>("/user/me")
      .then((response) => {
        const userData = response.data || response; 
        setNickname(userData.nickname);
        setOriginalNickname(userData.nickname);
        if (userData.profileImg) setPreviewUrl(userData.profileImg);
      })
      .catch(() => Alert.alert("오류", "사용자 정보를 불러오는데 실패했습니다."))
      .finally(() => setFetching(false));
  }, []);

  // 사진첩 열기 로직
  const pickImage = async () => {
    // 권한 요청
    const permissionResult = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (permissionResult.granted === false) {
      Alert.alert("권한 필요", "사진을 업로드하려면 갤러리 접근 권한이 필요합니다.");
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.8,
    });

    if (!result.canceled && result.assets && result.assets.length > 0) {
      setProfileImgUri(result.assets[0].uri);
      setPreviewUrl(result.assets[0].uri);
    }
  };

  const handleNicknameChange = (text: string) => {
    setNickname(text);
    if (text === originalNickname) {
      setIsChecked(true);
    } else {
      setIsChecked(false);
    }
  };

  const checkNickname = async () => {
    if (!nickname.trim()) return Alert.alert("알림", "닉네임을 입력해주세요.");
    if (nickname === originalNickname) {
      setIsChecked(true);
      return Alert.alert("알림", "현재 사용 중인 닉네임입니다.");
    }

    try {
      const response = await fetchApi<any>(`/auth/check-nickname?nickname=${nickname}`);
      if (response.data) {
        Alert.alert("실패", "이미 사용 중인 닉네임입니다.");
        setIsChecked(false);
      } else {
        Alert.alert("성공", "사용 가능한 닉네임입니다.");
        setIsChecked(true);
      }
    } catch (error) {
      Alert.alert("오류", "중복 확인 중 오류가 발생했습니다.");
    }
  };

  const handleSubmit = async () => {
    if (!isChecked) return Alert.alert("알림", "닉네임 중복 확인을 해주세요.");
    
    setLoading(true);
    
    // React Native의 FormData 처리 방식
    const formData = new FormData();
    formData.append("nickname", nickname);
    
    if (profileImgUri) {
      const filename = profileImgUri.split('/').pop();
      const match = /\.(\w+)$/.exec(filename || '');
      const type = match ? `image/${match[1]}` : `image`;
      
      // RN에서는 append 시 any로 캐스팅하거나 특수 객체를 넘겨야 동작함
      formData.append('profileImg', { uri: profileImgUri, name: filename, type } as any);
    }

    try {
      await fetchApi("/user/profile", {
        method: "PUT",
        body: formData,
        // RN fetch에서는 FormData 전송 시 Content-Type을 수동 지정하지 않아야 Boundary가 자동 설정됨
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'multipart/form-data', 
        }
      });
      Alert.alert("성공", "프로필이 수정되었습니다.", [
        { text: "확인", onPress: () => router.back() }
      ]);
    } catch (error: any) {
      Alert.alert("수정 실패", error.message || "오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  if (fetching) return <View style={styles.centerContainer}><ActivityIndicator size="large" color="#111" /></View>;

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView style={{flex: 1}} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.container}>
          <Text style={styles.pageTitle}>프로필 수정</Text>

          <View style={styles.profileSection}>
            <TouchableOpacity onPress={pickImage} style={styles.imageWrapper}>
              <Image source={{ uri: previewUrl }} style={styles.profileImage} />
              <View style={styles.cameraIconBadge}>
                <Ionicons name="camera" size={16} color="#fff" />
              </View>
            </TouchableOpacity>
            <Text style={styles.imageHint}>사진을 눌러 변경하세요</Text>
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>닉네임</Text>
            <View style={styles.row}>
              <TextInput 
                style={[
                  styles.input, 
                  nickname !== originalNickname && (isChecked ? styles.inputSuccess : styles.inputError)
                ]} 
                value={nickname} 
                onChangeText={handleNicknameChange} 
              />
              <TouchableOpacity 
                style={[styles.checkBtn, (nickname === originalNickname || !nickname.trim()) && styles.disabledBtn]} 
                onPress={checkNickname}
                disabled={nickname === originalNickname || !nickname.trim()}
              >
                <Text style={styles.checkBtnText}>중복확인</Text>
              </TouchableOpacity>
            </View>

            {nickname !== originalNickname && (
              <View style={styles.statusRow}>
                <Ionicons name={isChecked ? "checkmark-circle" : "close-circle"} size={16} color={isChecked ? "#10b981" : "#ef4444"} />
                <Text style={[styles.statusText, { color: isChecked ? "#10b981" : "#ef4444" }]}>
                  {isChecked ? "사용 가능한 닉네임입니다." : "중복 확인이 필요합니다."}
                </Text>
              </View>
            )}
          </View>

          <View style={{ flex: 1 }} />

          <View style={styles.bottomBtns}>
            <TouchableOpacity style={styles.cancelBtn} onPress={() => router.back()}>
              <Text style={styles.cancelBtnText}>취소</Text>
            </TouchableOpacity>
            <TouchableOpacity 
              style={[styles.submitBtn, (!isChecked || loading) && styles.disabledBtn]} 
              onPress={handleSubmit} 
              disabled={!isChecked || loading}
            >
              {loading ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.submitBtnText}>수정 완료</Text>
              )}
            </TouchableOpacity>
          </View>

        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fff' },
  centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  container: { flex: 1, padding: 24 },
  pageTitle: { fontSize: 24, fontWeight: 'bold', color: '#111', marginBottom: 30, textAlign: 'center' },
  profileSection: { alignItems: 'center', marginBottom: 40 },
  imageWrapper: { position: 'relative' },
  profileImage: { width: 120, height: 120, borderRadius: 60, borderWidth: 1, borderColor: '#e5e7eb' },
  cameraIconBadge: { position: 'absolute', bottom: 0, right: 0, backgroundColor: '#111', width: 32, height: 32, borderRadius: 16, justifyContent: 'center', alignItems: 'center', borderWidth: 2, borderColor: '#fff' },
  imageHint: { marginTop: 12, fontSize: 13, color: '#9ca3af' },
  inputGroup: { marginBottom: 20 },
  label: { fontSize: 14, fontWeight: 'bold', color: '#4b5563', marginBottom: 8 },
  row: { flexDirection: 'row', gap: 10 },
  input: { flex: 1, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, paddingHorizontal: 16, paddingVertical: 14, fontSize: 15, backgroundColor: '#fff' },
  inputSuccess: { borderColor: '#10b981' },
  inputError: { borderColor: '#ef4444' },
  checkBtn: { justifyContent: 'center', alignItems: 'center', paddingHorizontal: 16, borderRadius: 10, backgroundColor: '#f3f4f6', borderWidth: 1, borderColor: '#e5e7eb' },
  checkBtnText: { fontSize: 14, fontWeight: 'bold', color: '#374151' },
  disabledBtn: { opacity: 0.5 },
  statusRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 8 },
  statusText: { fontSize: 13, fontWeight: '500' },
  bottomBtns: { flexDirection: 'row', gap: 12, marginBottom: 20 },
  cancelBtn: { flex: 1, paddingVertical: 16, borderRadius: 12, alignItems: 'center', backgroundColor: '#f3f4f6' },
  cancelBtnText: { color: '#4b5563', fontSize: 15, fontWeight: 'bold' },
  submitBtn: { flex: 2, backgroundColor: '#111', paddingVertical: 16, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  submitBtnText: { color: '#fff', fontSize: 15, fontWeight: 'bold' }
});